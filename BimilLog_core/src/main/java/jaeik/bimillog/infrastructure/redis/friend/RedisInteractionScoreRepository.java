package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.springframework.data.redis.core.ScanOptions.scanOptions;

/**
 * <h2>상호작용 점수 Redis 캐시 저장소 (ZSet 기반)</h2>
 * <p>Redis ZSet을 사용하여 회원 간 상호작용 점수를 캐싱합니다.</p>
 * <p>Key: interaction:{memberId}, Member: targetId, Score: 점수</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Repository
@RequiredArgsConstructor
public class RedisInteractionScoreRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String INTERACTION_PREFIX = "interaction:"; // 상호 작용 점수 테이블(ZSet) 키 접두사
    private static final int PIPELINE_BATCH_SIZE = 500;
    private static final RedisScript<Long> INTERACTION_SCORE_ADD_SCRIPT; // 상호 작용 점수 증가 Lua Script
    private static final RedisScript<Long> INTERACTION_SCORE_DECAY_SCRIPT; // 상호 작용 점수 지수 감쇠 Lua Script
    private static final Long IDEMPOTENCY_TTL_SECONDS = 60 * 60L; // 멱등성 키 TTL 1시간
    private static final Double INTERACTION_SCORE_THRESHOLD = 0.2; // 상호 작용 점수 삭제 임계값
    private static final Double INTERACTION_SCORE_DEFAULT = 0.5; // 상호 작용 점수 증가 기본 값
    private static final Double INTERACTION_SCORE_LIMIT = 9.5; // 상호 작용 점수 증가 가능 최대값 (최대값은 10점)
    private static final Double INTERACTION_SCORE_DECAY_RATE = 0.95; // 상호 작용 점수 지수 감쇠율 (1일마다 0.95)


    static {
        String luaScript = """
            local key1 = KEYS[1]
            local key2 = KEYS[2]
            local idempotencyKey = KEYS[3]

            local member1 = tostring(ARGV[1])
            local member2 = tostring(ARGV[2])
            local increment = tonumber(ARGV[3])
            local maxScore = tonumber(ARGV[4])
            local ttl = tonumber(ARGV[5])

            -- 이미 처리된 이벤트인지 확인 (SET NX EX)
            if redis.call('SET', idempotencyKey, 1, 'NX', 'EX', ttl) == false then
                return 0
            end

            -- 점수 증가 (ZSet)
            local current1 = redis.call('ZSCORE', key1, member1)
            if not current1 or tonumber(current1) <= maxScore then
                redis.call('ZINCRBY', key1, increment, member1)
            end

            local current2 = redis.call('ZSCORE', key2, member2)
            if not current2 or tonumber(current2) <= maxScore then
                redis.call('ZINCRBY', key2, increment, member2)
            end

            return 1
        """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        INTERACTION_SCORE_ADD_SCRIPT = script;

        // 지수 감쇠 Lua Script: ZSet의 모든 점수에 감쇠율을 곱함
        String decayLuaScript = """
            local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES')
            for i = 1, #members, 2 do
                local member = members[i]
                local score = tonumber(members[i + 1])
                local newScore = score * tonumber(ARGV[1])
                redis.call('ZADD', KEYS[1], newScore, member)
            end
            return redis.call('ZCARD', KEYS[1])
            """;
        DefaultRedisScript<Long> decayScript = new DefaultRedisScript<>();
        decayScript.setScriptText(decayLuaScript);
        decayScript.setResultType(Long.class);
        INTERACTION_SCORE_DECAY_SCRIPT = decayScript;
    }

    /**
     * 상호작용 점수 추가 (멱등성 보장)
     * <p>비즈니스 키 기반 이벤트 ID를 사용하여 중복 처리를 방지합니다.</p>
     * <p>동일한 이벤트가 여러 번 호출되어도 점수는 한 번만 증가합니다.</p>
     *
     * @param memberId 회원 ID
     * @param interactionMemberId 상호작용 대상 회원 ID
     * @param idempotencyKey 멱등성 보장을 위한 비즈니스 키 (예: POST_LIKE:postId:likerId)
     * @return true: 점수 증가됨, false: 이미 처리된 이벤트
     */
    public boolean addInteractionScore(Long memberId, Long interactionMemberId, String idempotencyKey) {
        String key1 = INTERACTION_PREFIX + memberId;
        String key2 = INTERACTION_PREFIX + interactionMemberId;
        try {
            Long result = stringRedisTemplate.execute(
                    INTERACTION_SCORE_ADD_SCRIPT,
                    List.of(key1, key2, idempotencyKey), // 각각 KEYS[1],[2],[3]
                    interactionMemberId.toString(), // ARGV[1]
                    memberId.toString(), // ARGV[2]
                    String.valueOf(INTERACTION_SCORE_DEFAULT), // ARGV[3]
                    String.valueOf(INTERACTION_SCORE_LIMIT), // ARGV[4]
                    String.valueOf(IDEMPOTENCY_TTL_SECONDS) // ARGV[5]
            );
            return result == 1;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_WRITE_ERROR, e);
        }
    }

    /**
     * 회원의 상호작용 점수 상위 N개 조회 (점수 내림차순)
     *
     * @param memberId 기준 회원 ID
     * @param limit    조회할 개수
     * @return Set of (targetId, score) tuples
     */
    public Set<TypedTuple<Object>> getTopInteractionScores(Long memberId, int limit) {
        String key = INTERACTION_PREFIX + memberId;
        try {
            Set<TypedTuple<Object>> result = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
            return result != null ? result : Collections.emptySet();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_QUERY_ERROR, e);
        }
    }

    /**
     * 후보자들의 상호작용 점수만 파이프라인으로 일괄 조회
     *
     * @param memberId  기준 회원 ID
     * @param targetIds 점수를 조회할 대상(후보자) ID 목록
     * @return 파이프라인 결과 (targetIds 순서와 동일, null 포함 가능)
     */
    public List<Object> getInteractionScoresBatch(Long memberId, List<Long> targetIds) {
        String key = INTERACTION_PREFIX + memberId;
        List<Object> allResults = new ArrayList<>();

        try {
            for (int i = 0; i < targetIds.size(); i += PIPELINE_BATCH_SIZE) {
                List<Long> batch = targetIds.subList(i, Math.min(i + PIPELINE_BATCH_SIZE, targetIds.size()));

                List<Object> batchResults = redisTemplate.executePipelined((RedisConnection connection) -> {
                    for (Long targetId : batch) {
                        connection.zSetCommands().zScore(key.getBytes(StandardCharsets.UTF_8), targetId.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    return null;
                });
                allResults.addAll(batchResults);
            }
            return allResults;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_QUERY_ERROR, e);
        }
    }

    /**
     * 회원 탈퇴 시 상호작용 데이터 삭제
     * <p>SCAN 패턴 매칭으로 모든 상호작용 ZSet에서 탈퇴 회원 제거</p>
     */
    public void deleteInteractionKeyByWithdraw(Long withdrawMemberId) {
        try {
            // 1. 탈퇴 회원의 상호작용 ZSet 삭제
            String withdrawKey = INTERACTION_PREFIX + withdrawMemberId;
            redisTemplate.delete(withdrawKey);

            // 2. SCAN으로 모든 interaction 키 조회 후, 탈퇴 회원 데이터 제거
            String pattern = INTERACTION_PREFIX + "*";

            redisTemplate.execute((RedisConnection connection) -> {
                byte[] memberBytes = withdrawMemberId.toString().getBytes(StandardCharsets.UTF_8);

                connection.keyCommands().scan(scanOptions()
                        .match(pattern)
                        .count(100)
                        .build())
                        .forEachRemaining(key -> {
                            connection.zSetCommands().zRem(key, memberBytes);
                        });
                return null;
            });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_DELETE_ERROR, e);
        }
    }

    /**
     * 모든 상호작용 점수에 지수 감쇠 적용
     * <p>SCAN으로 모든 interaction:* 키를 찾아 Lua 스크립트로 점수 감쇠 적용</p>
     * <p>임계값(0.1) 이하의 점수는 삭제</p>
     *
     * @return 처리된 키 개수
     */
    public int applyInteractionScoreDecay() {
        String pattern = INTERACTION_PREFIX + "*";
        List<String> keys = new ArrayList<>();

        // 1. SCAN으로 모든 interaction 키 수집
        redisTemplate.execute((RedisConnection connection) -> {
            connection.keyCommands().scan(scanOptions()
                    .match(pattern)
                    .count(100)
                    .build())
                    .forEachRemaining(key -> keys.add(new String(key, StandardCharsets.UTF_8)));
            return null;
        });

        // 2. 각 키에 대해 지수 감쇠 적용
        for (String key : keys) {
            // Lua 스크립트로 점수 감쇠
            stringRedisTemplate.execute(
                    INTERACTION_SCORE_DECAY_SCRIPT,
                    List.of(key),
                    String.valueOf(INTERACTION_SCORE_DECAY_RATE)
            );

            // 임계값 이하의 멤버 제거
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, INTERACTION_SCORE_THRESHOLD);
        }

        return keys.size();
    }

    public void processScoreUp(RedisConnection connection, FriendEventDlq event) {
        String key1 = INTERACTION_PREFIX + event.getMemberId();
        String key2 = INTERACTION_PREFIX + event.getTargetId();
        double increment = event.getScore() != null ? event.getScore() : INTERACTION_SCORE_DEFAULT;

        connection.zSetCommands().zIncrBy(
                key1.getBytes(StandardCharsets.UTF_8),
                increment,
                event.getTargetId().toString().getBytes(StandardCharsets.UTF_8));
        connection.zSetCommands().zIncrBy(
                key2.getBytes(StandardCharsets.UTF_8),
                increment,
                event.getMemberId().toString().getBytes(StandardCharsets.UTF_8));
    }
}
