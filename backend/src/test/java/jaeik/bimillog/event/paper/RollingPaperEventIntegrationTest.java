package jaeik.bimillog.event.paper;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import jaeik.bimillog.testutil.EventTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
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
public class RollingPaperEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("롤링페이퍼 메시지 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void rollingPaperEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트사용자";
        RollingPaperEvent event = EventTestDataBuilder.createPaperEvent(paperOwnerId, userName);

        // When & Then
        publishAndVerify(event, () -> {
            verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
            verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));
        });
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - SSE 알림 실패")
    void eventProcessingWithException_SseNotificationFailure() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트사용자";
        RollingPaperEvent event = EventTestDataBuilder.createPaperEvent(paperOwnerId, userName);

        // SSE 알림 실패 시뮬레이션
        doThrow(new RuntimeException("SSE 알림 실패")).when(notificationSseUseCase).sendPaperPlantNotification(paperOwnerId, userName);

        // When & Then - SSE 실패 시 FCM은 호출되지 않음 (순차 실행이므로)
        publishAndExpectException(event, () -> verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName)));
    }

    @Test
    @DisplayName("예외 상황에서의 롤링페이퍼 이벤트 처리 - FCM 알림 실패")
    void eventProcessingWithException_FcmNotificationFailure() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트사용자";
        RollingPaperEvent event = EventTestDataBuilder.createPaperEvent(paperOwnerId, userName);

        // FCM 알림 실패 시뮬레이션
        doThrow(new RuntimeException("FCM 알림 실패")).when(notificationFcmUseCase).sendPaperPlantNotification(paperOwnerId);

        // When & Then - SSE는 성공하고 FCM 실패 시에도 둘 다 호출됨
        publishAndExpectException(event, () -> {
            verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
            verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));
        });
    }

}
