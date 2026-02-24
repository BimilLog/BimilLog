package jaeik.bimillog.infrastructure.redis.post;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jaeik.bimillog.domain.post.service.RealtimePostCacheService;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>레디스 실시간 인기글 저장소 어댑터</h2>
 * <p>실시간 인기글 ZSet 기반으로 인기글 ID를 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.1
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPostRealTimeAdapter {
    private final StringRedisTemplate stringRedisTemplate;
    private final RealtimeScoreFallbackStore fallbackStore;

    private static final String REALTIME_SCORE_KEY = RedisKey.REALTIME_POST_SCORE_KEY;
    public static final double REALTIME_POST_SCORE_DECAY_RATE = 0.97;
    public static final double REALTIME_POST_SCORE_THRESHOLD = 1.0;
    private static final int SYNC_BATCH_SIZE = 500;

    /**
     * <h3>실시간 인기글 조회</h3>
     * <p>실시간 인기글 에서 점수가 높은 게시글 ID순서대로 조회합니다.</p>
     * <p>서킷브레이커는 {@link RealtimePostCacheService}에서 관리합니다.</p>
     *
     * @return List<Long> 게시글 ID 목록 (점수 내림차순)
     */
    public List<Long> getRangePostId() {
        Set<String> set = stringRedisTemplate.opsForZSet().reverseRange(REALTIME_SCORE_KEY, 0, 4);
        if (set == null) {
            return Collections.emptyList();
        }
        return set.stream().map(Long::parseLong).collect(Collectors.toList());
    }

    /**
     * <h3>점수 증가</h3>
     * <p>Redis Sorted Set에서 특정 게시글의 점수를 증가시킵니다.</p>
     * <p>이벤트 리스너에서 조회/댓글/추천 이벤트 발생 시 호출됩니다.</p>
     * <p>Redis 장애 시 CircuitBreaker가 자동으로 fallbackMethod를 호출합니다.</p>
     *
     * @param postId 점수를 증가시킬 게시글 ID
     * @param score  증가시킬 점수 (조회: 2점, 댓글: 3점, 추천: 4점)
     */
    @CircuitBreaker(name = "realtimeRedis", fallbackMethod = "incrementScoreFallback")
    public void incrementRealtimePopularScore(Long postId, double score) {
        stringRedisTemplate.opsForZSet().incrementScore(REALTIME_SCORE_KEY, String.valueOf(postId), score);
    }

    /**
     * <h3>점수 증가 폴백</h3>
     * <p>서킷 OPEN 또는 Redis 실패 시 ConcurrentHashMap에 점수를 저장합니다.</p>
     *
     * @param postId 게시글 ID
     * @param score  증가시킬 점수
     * @param t      발생한 예외
     */
    @SuppressWarnings("unused")
    private void incrementScoreFallback(Long postId, double score, Throwable t) {
        log.warn("[CIRCUIT_FALLBACK] Redis 실패, 폴백 저장소 사용: postId={}, error={}", postId, t.getMessage());
        fallbackStore.incrementScore(postId, score);
    }

    /**
     * <h3>점수 감쇠 (스케쥴러)</h3>
     * <p>Redis Sorted Set의 모든 게시글 점수에 0.97를 곱하고, 임계값(1점) 이하의 게시글을 제거합니다.</p>
     * <p>PostCacheScheduler 스케줄러에서 10분마다 호출됩니다.</p>
     */
    public void applyRealtimePopularScoreDecay() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "local members = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES') " +
                "for i = 1, #members, 2 do " +
                "    local member = members[i] " +
                "    local score = tonumber(members[i + 1]) " +
                "    local newScore = score * tonumber(ARGV[1]) " +
                "    redis.call('ZADD', KEYS[1], newScore, member) " +
                "end " +
                "return 1"
        );
        script.setResultType(Long.class);

        // 1. 모든 항목의 점수에 0.97 곱하기
        stringRedisTemplate.execute(
                script,

                List.of(REALTIME_SCORE_KEY),
                String.valueOf(REALTIME_POST_SCORE_DECAY_RATE)
        );

        // 2. 임계값(1점) 이하의 게시글 제거
        stringRedisTemplate.opsForZSet().removeRangeByScore(REALTIME_SCORE_KEY, 0, REALTIME_POST_SCORE_THRESHOLD);
    }

    /**
     * <h3>캐시 삭제</h3>
     * <p>score:realtime Sorted Set에서 특정 postId를 삭제합니다.</p>
     * <p>게시글 삭제 시 실시간 인기글 점수 정리를 위해 호출됩니다.</p>
     * <p>서킷 OPEN 시 Caffeine 폴백 저장소에서 제거합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     */
    @CircuitBreaker(name = "realtimeRedis", fallbackMethod = "removePostIdFallback")
    public void removePostIdFromRealtimeScore(Long postId) {
        stringRedisTemplate.opsForZSet().remove(REALTIME_SCORE_KEY, String.valueOf(postId));
    }

    /**
     * <h3>캐시 삭제 폴백</h3>
     * <p>서킷 OPEN 또는 Redis 실패 시 Caffeine 폴백 저장소에서 게시글을 제거합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     * @param t      발생한 예외
     */
    @SuppressWarnings("unused")
    private void removePostIdFallback(Long postId, Throwable t) {
        log.warn("[CIRCUIT_FALLBACK] Redis 실패, 폴백 저장소에서 제거: postId={}, error={}", postId, t.getMessage());
        fallbackStore.removePost(postId);
    }

    /**
     * <h3>Redis ZSet Top N 점수 조회</h3>
     * <p>스케줄러가 Caffeine 웜업에 사용할 상위 N개 게시글의 점수를 반환합니다.</p>
     *
     * @param n 조회할 상위 개수
     * @return postId → score 맵 (점수 내림차순)
     */
    public Map<Long, Double> getTopNWithScores(int n) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(REALTIME_SCORE_KEY, 0, n - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t.getValue() != null && t.getScore() != null) {
                result.put(Long.parseLong(t.getValue()), t.getScore());
            }
        }
        return result;
    }

    /**
     * <h3>OPEN 구간 Caffeine 누적 점수를 Redis에 동기화</h3>
     * <p>서킷 CLOSED 전환 시 호출됩니다.</p>
     * <p>파이프라인 ZINCRBY로 Redis의 기존 점수에 Caffeine 누적 점수를 더합니다.</p>
     *
     * @param scores OPEN 구간에 Caffeine에 적립된 postId → score 맵
     */
    public void syncCaffeineScoresToRedis(Map<Long, Double> scores) {
        if (scores.isEmpty()) {
            return;
        }
        List<Map.Entry<Long, Double>> entries = new ArrayList<>(scores.entrySet());
        for (int i = 0; i < entries.size(); i += SYNC_BATCH_SIZE) {
            List<Map.Entry<Long, Double>> batch = entries.subList(i, Math.min(i + SYNC_BATCH_SIZE, entries.size()));
            stringRedisTemplate.executePipelined((RedisCallback<Object>) conn -> {
                StringRedisConnection c = (StringRedisConnection) conn;
                for (Map.Entry<Long, Double> entry : batch) {
                    c.zIncrBy(REALTIME_SCORE_KEY, entry.getValue(), String.valueOf(entry.getKey()));
                }
                return null;
            });
        }
        int batches = (entries.size() + SYNC_BATCH_SIZE - 1) / Math.max(SYNC_BATCH_SIZE, 1);
        log.info("[SYNC] Caffeine → Redis 점수 동기화 완료: {}건({}배치)", scores.size(), batches);
    }

    /**
     * <h3>OPEN 구간 삭제 로그를 Redis에 재처리</h3>
     * <p>서킷 CLOSED 전환 시 호출됩니다.</p>
     * <p>OPEN 구간에 삭제된 게시글을 Redis ZSet에서 제거합니다(best-effort).</p>
     *
     * @param deletedIds OPEN 구간에 삭제된 게시글 ID 목록
     */
    public void replayDeletionsToRedis(Set<Long> deletedIds) {
        try {
            List<Long> deletedList = new ArrayList<>(deletedIds);
            for (int i = 0; i < deletedList.size(); i += SYNC_BATCH_SIZE) {
                List<Long> batch = deletedList.subList(i, Math.min(i + SYNC_BATCH_SIZE, deletedList.size()));
                stringRedisTemplate.executePipelined((RedisCallback<Object>) conn -> {
                    StringRedisConnection c = (StringRedisConnection) conn;
                    for (Long postId : batch) {
                        c.zRem(REALTIME_SCORE_KEY, String.valueOf(postId));
                    }
                    return null;
                });
            }
            int batches = (deletedList.size() + SYNC_BATCH_SIZE - 1) / Math.max(SYNC_BATCH_SIZE, 1);
            log.info("[SYNC] 삭제 재처리 완료: {}건({}배치)", deletedIds.size(), batches);
        } catch (Exception e) {
            log.warn("[SYNC] 삭제 재처리 실패 (무시하고 진행): {}", e.getMessage());
        }
    }
}
