package jaeik.bimillog.springboot.nodb;

import jaeik.bimillog.domain.paper.event.MessageDeletedEvent;
import jaeik.bimillog.domain.paper.event.PaperViewedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.paper.listener.RealtimePaperPopularScoreListener;
import jaeik.bimillog.infrastructure.config.AsyncConfig;
import jaeik.bimillog.infrastructure.config.RetryConfig;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperUpdateAdapter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>RealtimePaperPopularScoreListener 재시도 테스트</h2>
 * <p>Redis 연결 실패 시 재시도 로직이 정상 동작하는지 검증</p>
 * <p>AsyncConfig를 포함하여 실제 비동기 환경에서 재시도를 검증</p>
 */
@DisplayName("RealtimePaperPopularScoreListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {RealtimePaperPopularScoreListener.class, RetryConfig.class, AsyncConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class RealtimePaperPopularScoreListenerRetryTest {

    @Autowired
    private RealtimePaperPopularScoreListener listener;

    @MockitoBean
    private RedisPaperUpdateAdapter redisPaperUpdateAdapter;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        Mockito.reset(redisPaperUpdateAdapter);
    }

    @Test
    @DisplayName("롤링페이퍼 조회 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handlePaperViewed_shouldRetryOnRedisConnectionFailure() {
        // Given
        PaperViewedEvent event = new PaperViewedEvent(1L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handlePaperViewed(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisPaperUpdateAdapter, times(MAX_ATTEMPTS))
                        .incrementRealtimePopularPaperScore(1L, 2.0));
    }

    @Test
    @DisplayName("메시지 작성 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleMessageCreated_shouldRetryOnRedisConnectionFailure() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(1L, "작성자");
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handleMessageCreated(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisPaperUpdateAdapter, times(MAX_ATTEMPTS))
                        .incrementRealtimePopularPaperScore(1L, 5.0));
    }

    @Test
    @DisplayName("메시지 삭제 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handleMessageDeleted_shouldRetryOnRedisConnectionFailure() {
        // Given
        MessageDeletedEvent event = new MessageDeletedEvent(1L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handleMessageDeleted(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisPaperUpdateAdapter, times(MAX_ATTEMPTS))
                        .incrementRealtimePopularPaperScore(1L, -5.0));
    }

    @Test
    @DisplayName("롤링페이퍼 조회 - 2회 실패 후 3회차에 성공")
    void handlePaperViewed_shouldSucceedAfterTwoFailures() {
        // Given
        PaperViewedEvent event = new PaperViewedEvent(100L);
        willThrow(new RedisConnectionFailureException("실패"))
                .willThrow(new RedisConnectionFailureException("실패"))
                .willDoNothing()
                .given(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(100L, 2.0);

        // When
        listener.handlePaperViewed(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisPaperUpdateAdapter, times(3))
                        .incrementRealtimePopularPaperScore(100L, 2.0));
    }

    @Test
    @DisplayName("메시지 작성 - 1회 성공 시 재시도 없음")
    void handleMessageCreated_shouldNotRetryOnSuccess() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(1L, "작성자");
        doNothing().when(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handleMessageCreated(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisPaperUpdateAdapter, times(1))
                        .incrementRealtimePopularPaperScore(1L, 5.0));
    }

    @Test
    @DisplayName("롤링페이퍼 조회 - 성공 시 실시간 인기 점수 증가")
    void handlePaperViewed_shouldIncrementScoreOnSuccess() {
        // Given
        PaperViewedEvent event = new PaperViewedEvent(1L);
        doNothing().when(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handlePaperViewed(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisPaperUpdateAdapter, times(1))
                        .incrementRealtimePopularPaperScore(1L, 2.0));
    }

    @Test
    @DisplayName("메시지 삭제 - 성공 시 실시간 인기 점수 감소")
    void handleMessageDeleted_shouldDecrementScoreOnSuccess() {
        // Given
        MessageDeletedEvent event = new MessageDeletedEvent(1L);
        doNothing().when(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handleMessageDeleted(event);

        // Then: 비동기 완료 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(redisPaperUpdateAdapter, times(1))
                        .incrementRealtimePopularPaperScore(1L, -5.0));
    }
}
