package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.*;
import static org.springframework.data.redis.core.ScanOptions.scanOptions;

/**
 * <h2>상호작용 점수 Redis 캐시 저장소</h2>
 * <p>Redis를 사용하여 회원 간 상호작용 점수를 캐싱합니다.</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Repository
@RequiredArgsConstructor
public class RedisInteractionScoreRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final int PIPELINE_BATCH_SIZE = 500;

    // 상호 작용 점수 증가 Lua Script (멱등성 보장)
    public static final RedisScript<Long> INTERACTION_SCORE_ADD_SCRIPT;
    static {
        String luaScript = """
            local key1 = KEYS[1]
            local key2 = KEYS[2]
            local idempotencyKey = KEYS[3]

            local member1 = tostring(ARGV[1])
            local member2 = tostring(ARGV[2])
            local increment = ARGV[3]
            local maxScore = tonumber(ARGV[4])
            local eventId = tostring(ARGV[5])
            local ttl = tonumber(ARGV[6])

            -- 이미 처리된 이벤트인지 확인
            if redis.call('SISMEMBER', idempotencyKey, eventId) == 1 then
                return 0  -- 이미 처리됨
            end

            -- 점수 증가
            local current1 = redis.call('HGET', key1, member1)
            if not current1 or tonumber(current1) <= maxScore then
                redis.call('HINCRBYFLOAT', key1, member1, increment)
            end

            local current2 = redis.call('HGET', key2, member2)
            if not current2 or tonumber(current2) <= maxScore then
                redis.call('HINCRBYFLOAT', key2, member2, increment)
            end

            -- 이벤트 ID를 처리 완료로 저장
            redis.call('SADD', idempotencyKey, eventId)
            redis.call('EXPIRE', idempotencyKey, ttl)

            return 1
        """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        INTERACTION_SCORE_ADD_SCRIPT = script;
    }

    /**
     * 회원의 모든 상호작용 점수 조회 (1촌이 없을 때 사용)
     *
     * @param memberId 기준 회원 ID
     * @return Map<대상ID, 점수> (점수가 있는 모든 대상)
     */
    public Map<Long, Double> getAllInteractionScores(Long memberId) {
        String key = INTERACTION_PREFIX + memberId;
        Map<Long, Double> resultMap = new HashMap<>();

        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String field = entry.getKey().toString();
                if (field.startsWith(INTERACTION_SUFFIX)) {
                    Long targetId = Long.valueOf(field.substring(INTERACTION_SUFFIX.length()));
                    String scoreValue = entry.getValue().toString();
                    resultMap.put(targetId, Double.valueOf(scoreValue));
                }
            }
            return resultMap;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_QUERY_ERROR, e);
        }
    }

    /**
     * 후보자들의 상호작용 점수만 파이프라인으로 일괄 조회
     *
     * @param memberId  기준 회원 ID
     * @param targetIds 점수를 조회할 대상(후보자) ID 목록
     * @return Map<대상ID, 점수>
     */
    public Map<Long, Double> getInteractionScoresBatch(Long memberId, List<Long> targetIds) {
        String key = INTERACTION_PREFIX + memberId;
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        Map<Long, Double> resultMap = new HashMap<>();

        try {
            for (int i = 0; i < targetIds.size(); i += PIPELINE_BATCH_SIZE) {
                List<Long> batch = targetIds.subList(i, Math.min(i + PIPELINE_BATCH_SIZE, targetIds.size()));

                List<Object> results = redisTemplate.executePipelined((RedisConnection connection) -> {
                    for (Long targetId : batch) {
                        String field = INTERACTION_SUFFIX + targetId;
                        connection.hashCommands().hGet(keyBytes, field.getBytes(StandardCharsets.UTF_8));
                    }
                    return null;
                });

                for (int j = 0; j < batch.size(); j++) {
                    Object scoreObj = results.get(j);
                    if (scoreObj != null) {
                        resultMap.put(batch.get(j), Double.parseDouble(scoreObj.toString()));
                    }
                }
            }
            return resultMap;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_QUERY_ERROR, e);
        }
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
        String member1 = INTERACTION_SUFFIX + interactionMemberId;
        String member2 = INTERACTION_SUFFIX + memberId;
        String idempotencySetKey = IDEMPOTENCY_PREFIX + memberId;
        try {
            Long result = stringRedisTemplate.execute(
                    INTERACTION_SCORE_ADD_SCRIPT,
                    List.of(key1, key2, idempotencySetKey),
                    member1,
                    member2,
                    String.valueOf(INTERACTION_SCORE_DEFAULT),
                    String.valueOf(INTERACTION_SCORE_LIMIT),
                    idempotencyKey,
                    String.valueOf(IDEMPOTENCY_TTL_SECONDS)
            );
            return result == 1;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_WRITE_ERROR, e);
        }
    }

    // 상호작용 삭제
    // 회원탈퇴시 호출 SCAN 패턴 매칭으로 상호작용 값 삭제
    public void deleteInteractionKeyByWithdraw(Long withdrawMemberId) {
        try {
            // 1. 탈퇴 회원의 상호작용 테이블 삭제
            String withdrawKey = INTERACTION_PREFIX + withdrawMemberId;
            redisTemplate.delete(withdrawKey);

            // 2. SCAN으로 모든 interaction 키 조회 후, 탈퇴 회원 데이터 제거
            String pattern = INTERACTION_PREFIX + "*";
            String deleteMember = INTERACTION_SUFFIX + withdrawMemberId;

            redisTemplate.execute((RedisConnection connection) -> {
                byte[] deleteMemberBytes = deleteMember.getBytes(StandardCharsets.UTF_8);

                // SCAN으로 패턴 매칭 키 조회
                connection.keyCommands().scan(scanOptions()
                        .match(pattern)
                        .count(100)
                        .build())
                        .forEachRemaining(key -> {
                            // 각 키에서 탈퇴 회원 제거
                            connection.hashCommands().hDel(key, deleteMemberBytes);
                        });
                return null;
            });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FRIEND_REDIS_INTERACTION_DELETE_ERROR, e);
        }
    }
}
