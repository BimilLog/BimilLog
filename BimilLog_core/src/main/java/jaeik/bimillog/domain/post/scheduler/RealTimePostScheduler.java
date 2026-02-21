package jaeik.bimillog.domain.post.scheduler;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * <h2>실시간 인기글 점수 감쇠 / Caffeine 웜업 스케줄러</h2>
 * <p>10분마다 지수감쇠, 1분마다 Redis Top 100 → Caffeine 웜업을 수행합니다.</p>
 * <p>서킷 CLOSED: Redis만 감쇠 (Caffeine은 1분 웜업으로 자동 반영) / 웜업 적용</p>
 * <p>서킷 OPEN: Caffeine만 감쇠, 웜업 스킵 (Redis 불가)</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true, message = "실시간 점수 감쇠 스케줄러")
@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimePostScheduler {

    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";
    private static final int CAFFEINE_WARM_UP_SIZE = 100;

    private boolean isRealtimeRedisCircuitOpen() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        CircuitBreaker.State state = cb.getState();
        return state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN;
    }

    /**
     * <h3>실시간 인기 게시글 점수 지수감쇠 적용</h3>
     * <p>서킷 CLOSED: Redis만 감쇠 (Caffeine은 1분마다 syncRedisToCaffeine이 덮어씀)</p>
     * <p>서킷 OPEN: Caffeine만 감쇠</p>
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
                redisPostRealTimeAdapter.applyRealtimePopularScoreDecay();
            } catch (Exception e) {
                log.error("Redis 실시간 인기글 점수 지수감쇠 적용 실패", e);
            }
        }
    }

    /**
     * <h3>Redis Top 100 → Caffeine 웜업</h3>
     * <p>1분마다 Redis 상위 100개 점수를 Caffeine에 반영하여 서킷 OPEN 시 콜드스타트를 방지합니다.</p>
     * <p>서킷 OPEN 중에는 Redis에 접근할 수 없으므로 스킵합니다.</p>
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void syncRedisToCaffeine() {
        if (isRealtimeRedisCircuitOpen()) {
            return;
        }
        try {
            Map<Long, Double> topScores = redisPostRealTimeAdapter.getTopNWithScores(CAFFEINE_WARM_UP_SIZE);
            if (!topScores.isEmpty()) {
                realtimeScoreFallbackStore.warmUp(topScores);
            }
        } catch (Exception e) {
            log.warn("Redis → Caffeine 웜업 실패: {}", e.getMessage());
        }
    }
}
