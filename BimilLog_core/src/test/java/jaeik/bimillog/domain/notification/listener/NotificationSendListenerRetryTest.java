package jaeik.bimillog.domain.notification.listener;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.event.AlarmSendEvent;
import jaeik.bimillog.domain.notification.service.FcmPushService;
import jaeik.bimillog.domain.notification.service.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationSendListener 재시도 테스트</h2>
 * <p>DB 관련 예외 발생 시 재시도 로직이 정상 동작하는지 검증</p>
 */
@DisplayName("NotificationSendListener 재시도 테스트")
@Tag("integration")
@SpringBootTest(classes = {NotificationSendListener.class, jaeik.bimillog.infrastructure.config.RetryConfig.class})
@TestPropertySource(properties = {
        "retry.max-attempts=3",
        "retry.backoff.delay=10",
        "retry.backoff.multiplier=1.0"
})
class NotificationSendListenerRetryTest {

    @Autowired
    private NotificationSendListener listener;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private FcmPushService fcmPushService;

    private static final int MAX_ATTEMPTS = 3;

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - SSE 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("SSE 알림 전송 - DB 예외 발생 시 재시도")
    void sendSSENotification_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        AlarmSendEvent event = AlarmSendEvent.ofComment(1L, "새 댓글", "/post/100", "댓글작성자");
        willThrow(exception)
                .given(sseService).sendNotification(anyLong(), any(NotificationType.class), anyString(), anyString());

        // When & Then: 예외가 발생하는 것을 확인하면서 실행
        assertThatThrownBy(() -> listener.sendSSENotification(event))
                .isInstanceOf(exception.getClass());

        // Then: 예외 발생 후 재시도 횟수만큼 호출되었는지 검증
        verify(sseService, times(MAX_ATTEMPTS))
                .sendNotification(1L, NotificationType.COMMENT, "새 댓글", "/post/100");
    }

    @ParameterizedTest(name = "{0} 발생 시 3회 재시도 - FCM 알림")
    @MethodSource("provideRetryableExceptions")
    @DisplayName("FCM 알림 전송 - DB 예외 발생 시 재시도")
    void sendFCMNotification_shouldRetryOnDatabaseExceptions(String exceptionName, RuntimeException exception) {
        // Given
        AlarmSendEvent event = AlarmSendEvent.ofComment(1L, "새 댓글", "/post/100", "댓글작성자");
        willThrow(exception)
                .given(fcmPushService).sendNotification(any(NotificationType.class), anyLong(), anyString(), any());

        // When & Then: 예외가 발생하는 것을 확인하면서 실행
        assertThatThrownBy(() -> listener.sendFCMNotification(event))
                .isInstanceOf(exception.getClass());

        // Then: 예외 발생 후 재시도 횟수만큼 호출되었는지 검증
        verify(fcmPushService, times(MAX_ATTEMPTS))
                .sendNotification(NotificationType.COMMENT, 1L, "댓글작성자", null);
    }

    private static Stream<Arguments> provideRetryableExceptions() {
        return Stream.of(
                Arguments.of("TransientDataAccessException",
                        new TransientDataAccessException("일시적 DB 오류") {}),
                Arguments.of("DataAccessResourceFailureException",
                        new DataAccessResourceFailureException("DB 리소스 획득 실패")),
                Arguments.of("QueryTimeoutException",
                        new QueryTimeoutException("쿼리 타임아웃"))
        );
    }

    @Test
    @DisplayName("SSE 알림 - 2회 실패 후 3회차에 성공")
    void sendSSENotification_shouldSucceedAfterTwoFailures() {
        // Given
        AlarmSendEvent event = AlarmSendEvent.of(1L, NotificationType.MESSAGE, "새 메시지", "/paper");
        willThrow(new DataAccessResourceFailureException("실패"))
                .willThrow(new QueryTimeoutException("타임아웃"))
                .willDoNothing()
                .given(sseService).sendNotification(1L, NotificationType.MESSAGE, "새 메시지", "/paper");

        // When
        listener.sendSSENotification(event);

        // Then
        verify(sseService, times(3))
                .sendNotification(1L, NotificationType.MESSAGE, "새 메시지", "/paper");
    }

    @Test
    @DisplayName("FCM 알림 - 1회 성공 시 재시도 없음")
    void sendFCMNotification_shouldNotRetryOnSuccess() {
        // Given
        AlarmSendEvent event = AlarmSendEvent.ofPostFeatured(1L, NotificationType.POST_FEATURED_WEEKLY, "인기글!", "/post/100", "제목");
        doNothing().when(fcmPushService).sendNotification(any(NotificationType.class), anyLong(), any(), anyString());

        // When
        listener.sendFCMNotification(event);

        // Then
        verify(fcmPushService, times(1))
                .sendNotification(NotificationType.POST_FEATURED_WEEKLY, 1L, null, "제목");
    }

    @Test
    @DisplayName("친구 요청 알림 - SSE와 FCM 모두 정상 동작")
    void shouldHandleFriendEventCorrectly() {
        // Given
        AlarmSendEvent event = AlarmSendEvent.ofFriend(1L, "친구 요청이 도착했습니다!", "/friends", "친구이름");
        doNothing().when(sseService).sendNotification(anyLong(), any(NotificationType.class), anyString(), anyString());
        doNothing().when(fcmPushService).sendNotification(any(NotificationType.class), anyLong(), anyString(), any());

        // When
        listener.sendSSENotification(event);
        listener.sendFCMNotification(event);

        // Then
        verify(sseService, times(1)).sendNotification(1L, NotificationType.FRIEND, "친구 요청이 도착했습니다!", "/friends");
        verify(fcmPushService, times(1)).sendNotification(NotificationType.FRIEND, 1L, "친구이름", null);
    }
}
