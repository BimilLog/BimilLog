package jaeik.bimillog.domain.post.scheduler;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>RealTimePostScheduler 테스트</h2>
 * <p>실시간 인기글 점수 지수감쇠 스케줄러의 서킷 분기, 예외 처리를 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealTimePostScheduler 테스트")
@Tag("unit")
class RealTimePostSchedulerTest {

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private RealTimePostScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).thenReturn(circuitBreaker);
    }

    // ==================== 점수 감쇠 ====================

    @Test
    @DisplayName("서킷 닫힘 → Redis ZSet에 감쇠 적용")
    void shouldApplyDecayToRedis_WhenCircuitClosed() {
        // Given
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        // When
        scheduler.applyRealtimeScoreDecay();

        // Then
        verify(redisRealTimePostAdapter).applyRealtimePopularScoreDecay();
        verify(realtimeScoreFallbackStore, never()).applyDecay();
    }

    @Test
    @DisplayName("서킷 OPEN → Caffeine 폴백 저장소에 감쇠 적용")
    void shouldApplyDecayToCaffeine_WhenCircuitOpen() {
        // Given
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        // When
        scheduler.applyRealtimeScoreDecay();

        // Then
        verify(realtimeScoreFallbackStore).applyDecay();
        verify(redisRealTimePostAdapter, never()).applyRealtimePopularScoreDecay();
    }

    @Test
    @DisplayName("서킷 FORCED_OPEN → Caffeine 폴백 저장소에 감쇠 적용")
    void shouldApplyDecayToCaffeine_WhenCircuitForcedOpen() {
        // Given
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.FORCED_OPEN);

        // When
        scheduler.applyRealtimeScoreDecay();

        // Then
        verify(realtimeScoreFallbackStore).applyDecay();
        verify(redisRealTimePostAdapter, never()).applyRealtimePopularScoreDecay();
    }

    @Test
    @DisplayName("Redis 감쇠 실패 시 예외를 잡아 로깅")
    void shouldCatchException_WhenRedisDecayFails() {
        // Given
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);
        willThrow(new RuntimeException("Redis 연결 실패"))
                .given(redisRealTimePostAdapter).applyRealtimePopularScoreDecay();

        // When - 예외가 전파되지 않아야 함
        scheduler.applyRealtimeScoreDecay();

        // Then
        verify(redisRealTimePostAdapter).applyRealtimePopularScoreDecay();
    }

    @Test
    @DisplayName("Caffeine 감쇠 실패 시 예외를 잡아 로깅")
    void shouldCatchException_WhenCaffeineDecayFails() {
        // Given
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);
        willThrow(new RuntimeException("Caffeine 오류"))
                .given(realtimeScoreFallbackStore).applyDecay();

        // When - 예외가 전파되지 않아야 함
        scheduler.applyRealtimeScoreDecay();

        // Then
        verify(realtimeScoreFallbackStore).applyDecay();
    }
}
