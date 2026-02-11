package jaeik.bimillog.domain.post.scheduler;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * <h2>실시간 인기글 점수 감쇠 스케줄러</h2>
 * <p>10분마다 실시간 인기글 점수에 지수감쇠를 적용합니다.</p>
 * <p>서킷브레이커 상태에 따라 Redis ZSet 또는 Caffeine 폴백 저장소에 감쇠를 적용합니다.</p>
 * <p>JSON LIST 재구축은 조회 시점에 ZSet과 비교하여 처리됩니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true, message = "실시간 점수 감쇠 스케줄러")
@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimePostScheduler {

    private final RedisRealTimePostAdapter redisRealTimePostAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";

    private boolean isRealtimeRedisCircuitOpen() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        CircuitBreaker.State state = cb.getState();
        return state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN;
    }

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용</h3>
     * <p>서킷 닫힘(Redis 정상): Redis ZSet에 감쇠 적용</p>
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
            } catch (Exception e) {
                log.error("Redis 실시간 인기글 점수 지수감쇠 적용 실패", e);
            }
        }
    }
}
