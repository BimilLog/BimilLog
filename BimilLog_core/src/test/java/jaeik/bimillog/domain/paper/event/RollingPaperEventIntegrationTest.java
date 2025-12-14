package jaeik.bimillog.domain.paper.event;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.service.FcmPushService;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>롤링페이퍼 메시지 이벤트 워크플로우 통합 테스트</h2>
 * <p>롤링페이퍼 메시지 이벤트 발생 시 SSE와 FCM 알림 전송 워크플로우를 검증하는 통합 테스트</p>
 * <p>이벤트 리스너의 비즈니스 로직과 의존성 간의 상호작용을 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("롤링페이퍼 메시지 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class RollingPaperEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private FcmPushService fcmPushService;

    @Test
    @DisplayName("롤링페이퍼 메시지 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void rollingPaperEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String memberName = "테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // When & Then
        publishAndVerify(event, () -> {
            verify(sseService).sendNotification(
                    eq(paperOwnerId),
                    eq(NotificationType.MESSAGE),
                    eq("롤링페이퍼에 메시지가 작성되었어요!"),
                    anyString());
            verify(fcmPushService).sendNotification(
                    eq(NotificationType.MESSAGE),
                    eq(paperOwnerId),
                    isNull(),
                    isNull());
        });
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - SSE 알림 실패")
    void eventProcessingWithException_SseNotificationFailure() {
        // Given
        Long paperOwnerId = 1L;
        String memberName = "테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // SSE 알림 실패 시뮬레이션
        doThrow(new RuntimeException("SSE 알림 실패"))
                .when(sseService).sendNotification(anyLong(), any(), anyString(), anyString());

        // When & Then - SSE 실패 시에도 비동기로 실행되므로 예외가 전파되지 않을 수 있음
        publishAndExpectException(event, () -> verify(sseService).sendNotification(
                eq(paperOwnerId),
                eq(NotificationType.MESSAGE),
                anyString(),
                anyString()));
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - FCM 알림 실패")
    void eventProcessingWithException_FcmNotificationFailure() {
        // Given
        Long paperOwnerId = 1L;
        String memberName = "테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, memberName);

        // FCM 알림 실패 시뮬레이션
        doThrow(new RuntimeException("FCM 알림 실패"))
                .when(fcmPushService).sendNotification(any(), anyLong(), any(), any());

        // When & Then - FCM 실패 시에도 비동기로 실행되므로 SSE는 독립적으로 동작
        publishAndExpectException(event, () -> {
            verify(sseService).sendNotification(
                    eq(paperOwnerId),
                    eq(NotificationType.MESSAGE),
                    eq("롤링페이퍼에 메시지가 작성되었어요!"),
                    anyString());
            verify(fcmPushService).sendNotification(
                    eq(NotificationType.MESSAGE),
                    eq(paperOwnerId),
                    isNull(),
                    isNull());
        });
    }

}
