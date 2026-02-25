package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.domain.friend.entity.jpa.FriendEventDlq;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;

import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.*;

import static jaeik.bimillog.infrastructure.redis.RedisKey.*;

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

    private static final Double INTERACTION_SCORE_THRESHOLD = 0.2;
    private static final Double INTERACTION_SCORE_DEFAULT = 0.5;
    private static final Double INTERACTION_SCORE_LIMIT = 9.5;
    private static final Double INTERACTION_SCORE_DECAY_RATE = 0.95;

    /**
     * 상호작용 점수 추가 (멱등성 보장)
     * <p>비즈니스 키 기반 이벤트 ID를 사용하여 중복 처리를 방지합니다.</p>
     * <p>동일한 이벤트가 여러 번 호출되어도 점수는 한 번만 증가합니다.</p>
     *
     * @param memberId            회원 ID
     * @param interactionMemberId 상호작용 대상 회원 ID
     * @param idempotencyKey      멱등성 보장을 위한 비즈니스 키 (예: POST_LIKE:postId:likerId)
     * @return true: 점수 증가됨, false: 이미 처리된 이벤트
     */
    public boolean addInteractionScore(Long memberId, Long interactionMemberId, String idempotencyKey) {
        final String INTERACTION_SCORE_ADD_SCRIPT = """
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
        DefaultRedisScript<Long> addScript = new DefaultRedisScript<>(INTERACTION_SCORE_ADD_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(
                addScript,
                List.of(createInteractionKey(memberId), createInteractionKey(interactionMemberId), idempotencyKey), // 각각 KEYS[1],[2],[3]
                interactionMemberId.toString(), // ARGV[1]
                memberId.toString(), // ARGV[2]
                String.valueOf(INTERACTION_SCORE_DEFAULT), // ARGV[3]
                String.valueOf(INTERACTION_SCORE_LIMIT), // ARGV[4]
                String.valueOf(IDEMPOTENCY_TTL_SECONDS) // ARGV[5]
        );
        return result == 1;
    }

    /**
     * 모든 상호작용 점수에 지수 감쇠 적용
     * <p>SCAN으로 모든 interaction:* 키를 찾아 Lua 스크립트로 점수 감쇠 적용</p>
     * <p>임계값(0.1) 이하의 점수는 삭제</p>
     *
     */
    public void applyInteractionScoreDecay() {
        final String INTERACTION_SCORE_DECAY_SCRIPT = """
                local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES')
                local decayRate = tonumber(ARGV[1])
                local threshold = tonumber(ARGV[2])
                for i = 1, #members, 2 do
                    local member = members[i]
                    local score = tonumber(members[i + 1])
                    local newScore = score * decayRate
                    if newScore <= threshold then
                        redis.call('ZREM', KEYS[1], member)
                    else
                        redis.call('ZADD', KEYS[1], newScore, member)
                    end
                end
                """;
        DefaultRedisScript<Void> decayScript = new DefaultRedisScript<>(INTERACTION_SCORE_DECAY_SCRIPT, Void.class);
        List<String> keys = new ArrayList<>();

        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(createAllInteractionKey())
                        .count(100)
                        .build())) {
            cursor.forEachRemaining(keys::add);
        }

        for (String key : keys) {
            stringRedisTemplate.execute(
                    decayScript,
                    Collections.singletonList(key),
                    String.valueOf(INTERACTION_SCORE_DECAY_RATE),
                    String.valueOf(INTERACTION_SCORE_THRESHOLD)
            );
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
        Set<TypedTuple<Object>> result = redisTemplate.opsForZSet().reverseRangeWithScores(createInteractionKey(memberId), 0, limit - 1);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * 후보자들의 상호작용 점수만 파이프라인으로 일괄 조회
     *
     * @param memberId  기준 회원 ID
     * @param targetIds 점수를 조회할 대상(후보자) ID 목록
     * @return 파이프라인 결과 (targetIds 순서와 동일, null 포함 가능)
     */
    public List<Object> getInteractionScoresBatch(Long memberId, List<Long> targetIds) {
        List<Object> allResults = new ArrayList<>();

        for (int i = 0; i < targetIds.size(); i += PIPELINE_BATCH_SIZE) {
            List<Long> batch = targetIds.subList(i, Math.min(i + PIPELINE_BATCH_SIZE, targetIds.size()));

            List<Object> batchResults = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Long targetId : batch) {
                    connection.zSetCommands().zScore(
                            createInteractionKey(memberId).getBytes(StandardCharsets.UTF_8),
                            String.valueOf(targetId).getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });

            allResults.addAll(batchResults);
        }
        return allResults;
    }

    /**
     * 회원 탈퇴 시 상호작용 데이터 삭제
     * <p>SCAN 패턴 매칭으로 모든 상호작용 ZSet에서 탈퇴 회원 제거</p>
     */
    public void deleteInteractionKeyByWithdraw(Long withdrawMemberId) {
        // 1. 탈퇴 회원의 상호작용 ZSet 삭제
        stringRedisTemplate.delete(createInteractionKey(withdrawMemberId));

        // 2. SCAN으로 모든 interaction 키 조회 후, 탈퇴 회원 데이터 제거
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(createAllInteractionKey())
                        .count(100)
                        .build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                stringRedisTemplate.opsForZSet().remove(key, String.valueOf(withdrawMemberId));
            }
        }
    }

    /**
     * DLQ 파이프라인 복구 — 상호작용 점수 증가
     */
    public void processScoreUp(RedisConnection connection, FriendEventDlq event) {
        double increment = event.getScore() != null ? event.getScore() : INTERACTION_SCORE_DEFAULT;

        connection.zSetCommands().zIncrBy(
                createInteractionKey(event.getMemberId()).getBytes(StandardCharsets.UTF_8),
                increment,
                event.getTargetId().toString().getBytes(StandardCharsets.UTF_8));
        connection.zSetCommands().zIncrBy(
                createInteractionKey(event.getTargetId()).getBytes(StandardCharsets.UTF_8),
                increment,
                event.getMemberId().toString().getBytes(StandardCharsets.UTF_8));
    }
}
