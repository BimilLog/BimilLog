package jaeik.bimillog.infrastructure.adapter.notification.in.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * <h2>알림 이벤트 리스너 테스트</h2>
 * <p>NotificationEventListener의 단위 테스트</p>
 * <p>각 이벤트 타입별 알림 처리 로직 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("알림 이벤트 리스너 테스트")
class NotificationGenerateListenerTest {

    @Mock
    private NotificationSseUseCase notificationSseUseCase;

    @Mock
    private NotificationFcmUseCase notificationFcmUseCase;

    @InjectMocks
    private NotificationGenerateListener notificationGenerateListener;

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - SSE와 FCM 알림 전송")
    void handleCommentCreatedEvent_ShouldSendNotifications() {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스트사용자";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(postUserId, commenterName, postId);

        // When
        notificationGenerateListener.handleCommentCreatedEvent(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(eq(postUserId), eq(commenterName));
    }

    @Test
    @DisplayName("인기글 선정 이벤트 처리 - SSE와 FCM 알림 전송")
    void handlePostFeaturedEvent_ShouldSendNotifications() {
        // Given
        Long userId = 1L;
        String sseMessage = "SSE 메시지";
        Long postId = 100L;
        String fcmTitle = "FCM 제목";
        String fcmBody = "FCM 내용";
        PostFeaturedEvent event = new PostFeaturedEvent(userId, sseMessage, postId, fcmTitle, fcmBody);

        // When
        notificationGenerateListener.handlePostFeaturedEvent(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(eq(userId), eq(sseMessage), eq(postId));
        verify(notificationFcmUseCase).sendPostFeaturedNotification(eq(userId), eq(fcmTitle), eq(fcmBody));
    }

    @Test
    @DisplayName("롤링페이퍼 이벤트 처리 - SSE와 FCM 알림 전송")
    void handleRollingPaperEvent_ShouldSendNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "테스트사용자";
        RollingPaperEvent event = new RollingPaperEvent(paperOwnerId, userName);

        // When
        notificationGenerateListener.handleRollingPaperEvent(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(eq(paperOwnerId), eq(userName));
        verify(notificationFcmUseCase).sendPaperPlantNotification(eq(paperOwnerId));
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - null 값들 처리")
    void handleCommentCreatedEvent_WithNullValues() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(null, null, null);

        // When
        notificationGenerateListener.handleCommentCreatedEvent(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(eq(null), eq(null), eq(null));
        verify(notificationFcmUseCase).sendCommentNotification(eq(null), eq(null));
    }

    @Test
    @DisplayName("인기글 선정 이벤트 처리 - null 값들 처리")
    void handlePostFeaturedEvent_WithNullValues() {
        // Given
        PostFeaturedEvent event = new PostFeaturedEvent(null, null, null, null, null);

        // When
        notificationGenerateListener.handlePostFeaturedEvent(event);

        // Then
        verify(notificationSseUseCase).sendPostFeaturedNotification(eq(null), eq(null), eq(null));
        verify(notificationFcmUseCase).sendPostFeaturedNotification(eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("롤링페이퍼 이벤트 처리 - null 값들 처리")
    void handleRollingPaperEvent_WithNullValues() {
        // Given
        RollingPaperEvent event = new RollingPaperEvent(null, null);

        // When
        notificationGenerateListener.handleRollingPaperEvent(event);

        // Then
        verify(notificationSseUseCase).sendPaperPlantNotification(eq(null), eq(null));
        verify(notificationFcmUseCase).sendPaperPlantNotification(eq(null));
    }

    @Test
    @DisplayName("댓글 생성 이벤트 처리 - 다양한 데이터 값들")
    void handleCommentCreatedEvent_WithVariousData() {
        // Given
        Long postUserId = 999L;
        String commenterName = "특별한사용자";
        Long postId = 12345L;
        CommentCreatedEvent event = new CommentCreatedEvent(postUserId, commenterName, postId);

        // When
        notificationGenerateListener.handleCommentCreatedEvent(event);

        // Then
        verify(notificationSseUseCase).sendCommentNotification(eq(postUserId), eq(commenterName), eq(postId));
        verify(notificationFcmUseCase).sendCommentNotification(eq(postUserId), eq(commenterName));
    }
}