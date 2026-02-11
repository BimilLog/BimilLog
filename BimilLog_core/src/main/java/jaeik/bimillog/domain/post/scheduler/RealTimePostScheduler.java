package jaeik.bimillog.domain.post.scheduler;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>실시간 인기글 점수 감쇠 스케줄러</h2>
 * <p>10분마다 실시간 인기글 점수에 지수감쇠를 적용합니다.</p>
 * <p>서킷브레이커 상태에 따라 Redis ZSet 또는 Caffeine 폴백 저장소에 감쇠를 적용합니다.</p>
 * <p>감쇠 후 ZSet에서 top ID를 조회하여 JSON LIST를 재구축합니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, logExecutionTime = true, message = "실시간 점수 감쇠 스케줄러")
@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimePostScheduler {

    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RedisPostJsonListAdapter redisPostJsonListAdapter;
    private final PostQueryRepository postQueryRepository;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int REALTIME_TOP_N = 5;

    private boolean isRealtimeRedisCircuitOpen() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        CircuitBreaker.State state = cb.getState();
        return state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN;
    }

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용 + JSON LIST 재구축</h3>
     * <p>서킷 닫힘(Redis 정상): Redis ZSet에 감쇠 적용 후 JSON LIST 재구축</p>
     * <p>서킷 열림(Redis 장애): Caffeine 폴백 저장소에 감쇠 적용</p>
     */
    @Scheduled(fixedRate = 60000 * 10) // 10분마다
    public void applyRealtimeScoreDecay() {
        if (isRealtimeRedisCircuitOpen()) {
            try {
                realtimeScoreFallbackStore.applyDecay();
            } catch (Exception e) {
                log.error("Fallback 저장소 지수감쇠 적용 실패", e);
            }
        } else {
            try {
                redisRealTimePostAdapter.applyRealtimePopularScoreDecay();
                rebuildRealtimeJsonList();
            } catch (Exception e) {
                log.error("Redis 실시간 인기글 점수 지수감쇠 적용 실패", e);
            }
        }
    }

    /**
     * ZSet에서 top N ID → DB 조회 → JSON LIST 전체 교체
     */
    private void rebuildRealtimeJsonList() {
        List<Long> topIds = redisRealTimePostAdapter.getRangePostId();
        if (topIds.isEmpty()) {
            return;
        }

        List<PostSimpleDetail> posts = postQueryRepository
                .findPostSimpleDetailsByIds(topIds, PageRequest.of(0, REALTIME_TOP_N))
                .getContent();

        if (!posts.isEmpty()) {
            redisPostJsonListAdapter.replaceAll(RedisKey.POST_REALTIME_JSON_KEY, posts, RedisKey.DEFAULT_CACHE_TTL);
            log.debug("[REALTIME] JSON LIST 재구축: {}개", posts.size());
        }
    }
}
