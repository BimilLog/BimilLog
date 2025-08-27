package jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler;

import jaeik.growfarm.domain.paper.event.RollingPaperEvent;
import jaeik.growfarm.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * <h2>롤링페이퍼 메시지 수신 이벤트 핸들러 테스트</h2>
 * <p>MessageEventHandler의 단위 테스트</p>
 * <p>롤링페이퍼 메시지 수신 시 SSE와 FCM 알림 전송 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("롤링페이퍼 메시지 수신 이벤트 핸들러 테스트")
class MessageEventHandlerTest {

    @Mock
    private NotificationSseUseCase notificationSseUseCase;

    @Mock
    private NotificationFcmUseCase notificationFcmUseCase;

    @InjectMocks
    private MessageEventHandler messageEventHandler;

    @Test
    @DisplayName("이벤트 지원 여부 확인 - RollingPaperEvent 지원")
    void supports_WithRollingPaperEvent_ShouldReturnTrue() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(this, 1L, "테스트사용자");

        // When
        boolean supports = messageEventHandler.supports(event);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("이벤트 지원 여부 확인 - 다른 이벤트는 미지원")
    void supports_WithOtherEvent_ShouldReturnFalse() {
        // Given
        ApplicationEvent otherEvent = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);

        // When
        boolean supports = messageEventHandler.supports(otherEvent);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("이벤트 지원 여부 확인 - null 이벤트")
    void supports_WithNullEvent_ShouldReturnFalse() {
        // When
        boolean supports = messageEventHandler.supports(null);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - SSE와 FCM 알림 전송")
    void handle_WithRollingPaperEvent_ShouldSendNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "메시지작성자";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - 다양한 이벤트 데이터")
    void handle_WithVariousEventData_ShouldSendCorrectNotifications() {
        // Given
        Long paperOwnerId = 999L;
        String userName = "익명의사용자";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - null 페이퍼 소유자 ID")
    void handle_WithNullPaperOwnerId_ShouldSendNotifications() {
        // Given
        Long paperOwnerId = null;
        String userName = "메시지작성자";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - null 사용자명")
    void handle_WithNullUserName_ShouldSendNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = null;
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - 빈 문자열 사용자명")
    void handle_WithEmptyUserName_ShouldSendNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - SSE 알림 실패 시에도 FCM 알림 전송")
    void handle_WhenSseNotificationFails_ShouldStillSendFcmNotification() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "메시지작성자";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);
        
        RuntimeException sseException = new RuntimeException("SSE 알림 실패");
        doThrow(sseException).when(notificationSseUseCase)
                .sendPaperPlantNotification(paperOwnerId, userName);

        // When & Then
        try {
            messageEventHandler.handle(event);
        } catch (RuntimeException e) {
            // SSE가 실패해도 FCM은 호출되어야 함
            verify(notificationSseUseCase).sendPaperPlantNotification(
                    eq(paperOwnerId), eq(userName));
            // FCM은 호출되지 않음 (SSE에서 예외 발생으로 중단)
            verifyNoInteractions(notificationFcmUseCase);
        }
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - FCM 알림 실패 시 예외 전파")
    void handle_WhenFcmNotificationFails_ShouldPropagateException() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "메시지작성자";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);
        
        RuntimeException fcmException = new RuntimeException("FCM 알림 실패");
        doThrow(fcmException).when(notificationFcmUseCase)
                .sendPaperPlantNotification(paperOwnerId);

        // When & Then
        try {
            messageEventHandler.handle(event);
        } catch (RuntimeException e) {
            // SSE는 정상 호출
            verify(notificationSseUseCase).sendPaperPlantNotification(
                    eq(paperOwnerId), eq(userName));
            // FCM도 호출됨
            verify(notificationFcmUseCase).sendPaperPlantNotification(
                    eq(paperOwnerId));
        }
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - 이벤트 데이터 검증")
    void handle_EventDataValidation() {
        // Given
        Long paperOwnerId = 123L;
        String userName = "테스트메시지작성자";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // 이벤트 데이터 검증
        assertThat(event.getPaperOwnerId()).isEqualTo(paperOwnerId);
        assertThat(event.getUserName()).isEqualTo(userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - 긴 사용자명")
    void handle_WithLongUserName_ShouldSendNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "매우긴사용자명".repeat(50);
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }

    @Test
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 처리 - 특수문자 포함 사용자명")
    void handle_WithSpecialCharactersInUserName_ShouldSendNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트@#$%^&*()사용자";
        
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        messageEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(
                eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(
                eq(paperOwnerId));
    }
}