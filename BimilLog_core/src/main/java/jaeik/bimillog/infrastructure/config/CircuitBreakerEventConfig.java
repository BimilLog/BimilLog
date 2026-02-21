package jaeik.bimillog.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * <h2>서킷브레이커 이벤트 설정</h2>
 * <p>서킷브레이커 상태 전환 시 필요한 후처리를 등록합니다.</p>
 * <p>realtimeRedis 서킷 CLOSED 전환 시:</p>
 * <ol>
 *   <li>OPEN 구간에 삭제된 게시글을 Redis에서 제거합니다.</li>
 *   <li>OPEN 구간에 Caffeine에 쌓인 점수를 Redis에 합산합니다.</li>
 *   <li>Caffeine 폴백 저장소를 초기화합니다.</li>
 * </ol>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerEventConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RealtimeScoreFallbackStore realtimeScoreFallbackStore;
    private final RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";

    @PostConstruct
    public void registerStateTransitionEvents() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        cb.getEventPublisher().onStateTransition(event -> {
            CircuitBreaker.StateTransition transition = event.getStateTransition();
            log.info("[CIRCUIT] realtimeRedis 상태 전환: {}", transition);

            if (transition.getToState() == CircuitBreaker.State.CLOSED) {
                Map<Long, Double> scores = realtimeScoreFallbackStore.getAllScores();
                Set<Long> deletedIds = realtimeScoreFallbackStore.getDeletedPostIds();

                if (!scores.isEmpty() || !deletedIds.isEmpty()) {
                    redisPostRealTimeAdapter.syncFallbackToRedis(scores, deletedIds);
                }

                realtimeScoreFallbackStore.clear();
                log.info("[CIRCUIT] Caffeine 폴백 저장소 초기화 완료 (Redis 복구)");
            }
        });
    }
}
