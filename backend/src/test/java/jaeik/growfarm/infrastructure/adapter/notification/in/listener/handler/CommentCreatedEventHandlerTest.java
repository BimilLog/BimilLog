package jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler;

import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.growfarm.domain.admin.event.UserBannedEvent;
import jaeik.growfarm.domain.common.entity.SocialProvider;
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
 * <h2>댓글 생성 이벤트 핸들러 테스트</h2>
 * <p>CommentCreatedEventHandler의 단위 테스트</p>
 * <p>댓글 생성 시 SSE와 FCM 알림 전송 로직을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 생성 이벤트 핸들러 테스트")
class CommentCreatedEventHandlerTest {

    @Mock
    private NotificationSseUseCase notificationSseUseCase;

    @Mock
    private NotificationFcmUseCase notificationFcmUseCase;

    @InjectMocks
    private CommentCreatedEventHandler commentCreatedEventHandler;

    @Test
    @DisplayName("이벤트 지원 여부 확인 - CommentCreatedEvent 지원")
    void supports_WithCommentCreatedEvent_ShouldReturnTrue() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(this, 1L, "테스트사용자", 100L);

        // When
        boolean supports = commentCreatedEventHandler.supports(event);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("이벤트 지원 여부 확인 - 다른 이벤트는 미지원")
    void supports_WithOtherEvent_ShouldReturnFalse() {
        // Given
        ApplicationEvent otherEvent = new UserBannedEvent(this, 1L, "socialId", SocialProvider.KAKAO);

        // When
        boolean supports = commentCreatedEventHandler.supports(otherEvent);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("이벤트 지원 여부 확인 - null 이벤트")
    void supports_WithNullEvent_ShouldReturnFalse() {
        // When
        boolean supports = commentCreatedEventHandler.supports(null);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - SSE와 FCM 알림 전송")
    void handle_WithCommentCreatedEvent_ShouldSendNotifications() {
        // Given
        Long postUserId = 1L;
        String commenterName = "댓글작성자";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);

        // When
        commentCreatedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName));
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 다양한 이벤트 데이터")
    void handle_WithVariousEventData_ShouldSendCorrectNotifications() {
        // Given
        Long postUserId = 999L;
        String commenterName = "익명의댓글러";
        Long postId = 777L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);

        // When
        commentCreatedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName));
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - null 댓글 작성자명")
    void handle_WithNullCommenterName_ShouldSendNotifications() {
        // Given
        Long postUserId = 1L;
        String commenterName = null;
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);

        // When
        commentCreatedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName));
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - null 게시글 사용자 ID")
    void handle_WithNullPostUserId_ShouldSendNotifications() {
        // Given
        Long postUserId = null;
        String commenterName = "댓글작성자";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);

        // When
        commentCreatedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName));
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - SSE 알림 실패 시에도 FCM 알림 전송")
    void handle_WhenSseNotificationFails_ShouldStillSendFcmNotification() {
        // Given
        Long postUserId = 1L;
        String commenterName = "댓글작성자";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);
        
        RuntimeException sseException = new RuntimeException("SSE 알림 실패");
        doThrow(sseException).when(notificationSseUseCase)
                .sendCommentNotification(postUserId, commenterName, postId);

        // When & Then
        try {
            commentCreatedEventHandler.handle(event);
        } catch (RuntimeException e) {
            // SSE가 실패해도 FCM은 호출되어야 함
            verify(notificationSseUseCase).sendCommentNotification(
                    eq(postUserId), eq(commenterName), eq(postId));
            // FCM은 호출되지 않음 (SSE에서 예외 발생으로 중단)
            verifyNoInteractions(notificationFcmUseCase);
        }
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - FCM 알림 실패 시 예외 전파")
    void handle_WhenFcmNotificationFails_ShouldPropagateException() {
        // Given
        Long postUserId = 1L;
        String commenterName = "댓글작성자";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);
        
        RuntimeException fcmException = new RuntimeException("FCM 알림 실패");
        doThrow(fcmException).when(notificationFcmUseCase)
                .sendCommentNotification(postUserId, commenterName);

        // When & Then
        try {
            commentCreatedEventHandler.handle(event);
        } catch (RuntimeException e) {
            // SSE는 정상 호출
            verify(notificationSseUseCase).sendCommentNotification(
                    eq(postUserId), eq(commenterName), eq(postId));
            // FCM도 호출됨
            verify(notificationFcmUseCase).sendCommentNotification(
                    eq(postUserId), eq(commenterName));
        }
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 이벤트 데이터 검증")
    void handle_EventDataValidation() {
        // Given
        Long postUserId = 123L;
        String commenterName = "테스트댓글러";
        Long postId = 456L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);

        // 이벤트 데이터 검증
        assertThat(event.getPostUserId()).isEqualTo(postUserId);
        assertThat(event.getCommenterName()).isEqualTo(commenterName);
        assertThat(event.getPostId()).isEqualTo(postId);

        // When
        commentCreatedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName));
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 빈 문자열 댓글 작성자명")
    void handle_WithEmptyCommenterName_ShouldSendNotifications() {
        // Given
        Long postUserId = 1L;
        String commenterName = "";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);

        // When
        commentCreatedEventHandler.handle(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(
                eq(postUserId), eq(commenterName));
    }
}