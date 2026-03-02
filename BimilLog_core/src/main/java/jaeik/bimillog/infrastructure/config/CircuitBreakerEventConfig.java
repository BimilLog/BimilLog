package jaeik.bimillog.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.async.CacheRealtimeSync;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>서킷브레이커 이벤트 설정</h2>
 * <p>서킷브레이커 상태 전환 시 필요한 후처리를 등록합니다.</p>
 * <p>realtimeRedis 서킷 CLOSED 전환 시:</p>
 * <ol>
 *   <li>OPEN 구간에 Caffeine에 누적된 증분 점수를 파이프라인 ZINCRBY로 Redis에 더합니다.</li>
 *   <li>OPEN 구간에 삭제된 게시글을 Redis에서 제거합니다.</li>
 * </ol>
 * <p>동기화는 {@link CacheRealtimeSync#syncCaffeineToRedis()}에서 비동기(circuitSyncExecutor)로 실행되어
 * 상태 전환을 트리거한 스레드(톰캣 워커 or 비동기)를 블로킹하지 않습니다.</p>
 *
 * @author Jaeik
 * @version 2.9.0
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerEventConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CacheRealtimeSync cacheRealtimeSync;

    private static final String REALTIME_REDIS_CIRCUIT = "realtimeRedis";

    @PostConstruct
    public void registerStateTransitionEvents() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(REALTIME_REDIS_CIRCUIT);
        cb.getEventPublisher().onStateTransition(event -> {
            CircuitBreaker.StateTransition transition = event.getStateTransition();
            log.info("[CIRCUIT] realtimeRedis 상태 전환: {}", transition);

            if (transition.getToState() == CircuitBreaker.State.CLOSED) {
                cacheRealtimeSync.syncCaffeineToRedis();
            }
        });
    }
}
