package jaeik.bimillog.unit.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.lettuce.core.RedisCommandTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.stream.Stream;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>realtimeRedis 서킷브레이커 예외별 개폐 테스트</h2>
 * <p>각 Redis 예외 상황에서 서킷브레이커가 정상적으로 실패를 기록하고 OPEN되는지 검증합니다.</p>
 * <p>application.properties의 realtimeRedis record-exceptions 설정과 동일한 구성으로 테스트합니다.</p>
 *
 * <h3>핵심 검증 포인트</h3>
 * <ul>
 *   <li>record-exceptions에 등록된 예외 → 서킷 OPEN</li>
 *   <li>ignore-exceptions에 등록된 예외 → 서킷에 기록되지 않음</li>
 *   <li>Spring Data Redis가 래핑한 예외 → 서킷 수신 여부 검증</li>
 * </ul>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Tag("unit")
@DisplayName("realtimeRedis 서킷브레이커 예외별 개폐 테스트")
class RealtimeRedisCircuitBreakerTripTest {

    private CircuitBreaker circuitBreaker;

    /**
     * application.properties의 realtimeRedis 서킷브레이커 설정과 동일하게 구성합니다.
     * automaticTransition만 false로 설정하여 테스트 중 자동 전환을 방지합니다.
     */
    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(false)
                .recordExceptions(
                        RedisConnectionFailureException.class,
                        RedisSystemException.class,
                        RedisCommandTimeoutException.class,
                        QueryTimeoutException.class
                )
                .ignoreExceptions(SerializationException.class)
                .build();

        circuitBreaker = CircuitBreaker.of("realtimeRedis-test", config);
    }

    /**
     * 서킷브레이커 데코레이터를 통해 예외를 발생시킵니다.
     * 실제 @CircuitBreaker AOP와 동일한 경로로 예외가 처리됩니다.
     */
    private void simulateFailure(RuntimeException exception) {
        try {
            CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
                throw exception;
            }).run();
        } catch (Exception ignored) {
        }
    }

    private void simulateSuccess() {
        CircuitBreaker.decorateRunnable(circuitBreaker, () -> {}).run();
    }

    // ==================== record-exceptions 등록 예외 → 서킷 OPEN ====================

    @ParameterizedTest(name = "{0} → 서킷 OPEN")
    @MethodSource("provideRecordedExceptions")
    @DisplayName("record-exceptions에 등록된 예외 3회 연속 발생 시 서킷 OPEN")
    void shouldOpenCircuit_WhenRecordedExceptionOccurs(String exceptionName, RuntimeException exception) {
        // Given: 초기 상태 CLOSED
        assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);

        // When: 3회 연속 실패 (minimumNumberOfCalls=3, failureRateThreshold=50%)
        for (int i = 0; i < 3; i++) {
            simulateFailure(exception);
        }

        // Then: 100% 실패율 → 서킷 OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(OPEN);
    }

    private static Stream<Arguments> provideRecordedExceptions() {
        return Stream.of(
                Arguments.of(
                        "RedisConnectionFailureException (Redis 완전 다운)",
                        new RedisConnectionFailureException("Redis 연결 실패")
                ),
                Arguments.of(
                        "RedisSystemException (Redis 시스템 오류)",
                        new RedisSystemException("Redis 시스템 오류", new RuntimeException("내부 원인"))
                ),
                Arguments.of(
                        "RedisCommandTimeoutException (Lettuce 원본 타임아웃)",
                        new RedisCommandTimeoutException("Command timed out after 500ms")
                ),
                Arguments.of(
                        "QueryTimeoutException (Spring이 래핑한 커맨드 타임아웃)",
                        new QueryTimeoutException("Command timed out after 500ms")
                )
        );
    }

    // ==================== ignore-exceptions → 서킷에 기록되지 않음 ====================

    @Test
    @DisplayName("SerializationException → ignore-exceptions → 서킷에 기록되지 않음")
    void shouldNotOpenCircuit_WhenIgnoredExceptionOccurs() {
        // Given
        assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);

        // When: SerializationException 5회 발생
        for (int i = 0; i < 5; i++) {
            simulateFailure(new SerializationException("직렬화 실패"));
        }

        // Then: ignore-exceptions에 등록되어 있으므로 무시 (성공도 실패도 아님)
        assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);
        // 호출 자체가 기록되지 않음
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
    }

    // ==================== 혼합 시나리오 ====================

    @Test
    @DisplayName("record 예외 2회 + 성공 1회 → 실패율 66.6% → 서킷 OPEN")
    void shouldOpenCircuit_WhenFailureRateExceedsThreshold() {
        // Given
        assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);

        // When: 실패 2회 + 성공 1회 = 실패율 66.6% (> 50%)
        simulateFailure(new RedisConnectionFailureException("연결 실패"));
        simulateFailure(new RedisSystemException("시스템 오류", new RuntimeException()));
        simulateSuccess();

        // Then: 66.6% > 50% 임계값 → 서킷 OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(OPEN);
    }

    @Test
    @DisplayName("record 예외 1회 + 성공 2회 → 실패율 33.3% → 서킷 CLOSED 유지")
    void shouldRemainClosed_WhenFailureRateBelowThreshold() {
        // Given
        assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);

        // When: 실패 1회 + 성공 2회 = 실패율 33.3% (< 50%)
        simulateFailure(new RedisConnectionFailureException("연결 실패"));
        simulateSuccess();
        simulateSuccess();

        // Then: 33.3% < 50% 임계값 → 서킷 CLOSED 유지
        assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThan(0.0f);
    }

    @Test
    @DisplayName("RedisConnectionFailureException + QueryTimeoutException 혼합 → 모두 실패로 기록되어 서킷 OPEN")
    void shouldOpenCircuit_WhenQueryTimeoutExceptionMixedWithRecordedExceptions() {
        // Given
        assertThat(circuitBreaker.getState()).isEqualTo(CLOSED);

        // When: RedisConnectionFailureException 1회 + QueryTimeoutException 2회
        // QueryTimeoutException도 record-exceptions에 등록되어 있으므로 모두 실패로 기록
        simulateFailure(new RedisConnectionFailureException("연결 실패"));
        simulateFailure(new QueryTimeoutException("커맨드 타임아웃"));
        simulateFailure(new QueryTimeoutException("커맨드 타임아웃"));

        // Then: 실패율 100% → 서킷 OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(OPEN);
    }
}
