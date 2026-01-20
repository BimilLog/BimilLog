package jaeik.bimillog.domain.paper.listener;

import jaeik.bimillog.domain.paper.event.MessageDeletedEvent;
import jaeik.bimillog.domain.paper.event.PaperViewedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperUpdateAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>RealtimePaperPopularScoreListener 재시도 테스트</h2>
 * <p>Redis 연결 실패 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("RealtimePaperPopularScoreListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {RealtimePaperPopularScoreListener.class, jaeik.bimillog.infrastructure.config.RetryConfig.class})
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

    @Test
    @DisplayName("롤링페이퍼 조회 - RedisConnectionFailureException 발생 시 3회 재시도")
    void handlePaperViewed_shouldRetryOnRedisConnectionFailure() {
        // Given
        PaperViewedEvent event = new PaperViewedEvent(1L);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handlePaperViewed(event);

        // Then
        verify(redisPaperUpdateAdapter, times(MAX_ATTEMPTS))
                .incrementRealtimePopularPaperScore(1L, 2.0);
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

        // Then
        verify(redisPaperUpdateAdapter, times(MAX_ATTEMPTS))
                .incrementRealtimePopularPaperScore(1L, 5.0);
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

        // Then
        verify(redisPaperUpdateAdapter, times(MAX_ATTEMPTS))
                .incrementRealtimePopularPaperScore(1L, -5.0);
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

        // Then
        verify(redisPaperUpdateAdapter, times(3))
                .incrementRealtimePopularPaperScore(100L, 2.0);
    }

    @Test
    @DisplayName("메시지 작성 - 1회 성공 시 재시도 없음")
    void handleMessageCreated_shouldNotRetryOnSuccess() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(1L, "작성자");
        doNothing().when(redisPaperUpdateAdapter).incrementRealtimePopularPaperScore(anyLong(), anyDouble());

        // When
        listener.handleMessageCreated(event);

        // Then
        verify(redisPaperUpdateAdapter, times(1))
                .incrementRealtimePopularPaperScore(1L, 5.0);
    }
}
