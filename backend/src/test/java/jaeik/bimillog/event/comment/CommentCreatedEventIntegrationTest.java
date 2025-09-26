package jaeik.bimillog.event.comment;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * <h2>댓글 생성 이벤트 워크플로우 통합 테스트</h2>
 * <p>댓글 생성 시 NotificationGenerateListener가 SSE와 FCM 알림을 전송하는 워크플로우를 검증</p>
 * <p>이벤트 리스너의 책임: 이벤트 수신 → 적절한 유스케이스 호출 검증</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@DisplayName("댓글 생성 이벤트 워크플로우 통합 테스트")
@Tag("fast-integration")
class CommentCreatedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("댓글 생성 이벤트 발생 시 SSE와 FCM 알림 유스케이스 호출 검증")
    void commentCreatedEventWorkflow_ShouldCompleteNotifications() {
        // Given
        var event = new CommentCreatedEvent(1L, "댓글작성자", 100L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(notificationSseUseCase).sendCommentNotification(
                    eq(1L), eq("댓글작성자"), eq(100L));
            verify(notificationFcmUseCase).sendCommentNotification(
                    eq(1L), eq("댓글작성자"));
        });
    }

    @Test
    @DisplayName("다중 댓글 생성 이벤트 처리 - 각 이벤트가 독립적으로 처리됨")
    void multipleCommentCreatedEvents_ShouldProcessIndependently() {
        // Given - 다양한 댓글 이벤트 생성
        var events = new java.util.ArrayList<CommentCreatedEvent>();
        events.add(new CommentCreatedEvent(1L, "댓글작성자1", 100L));  // 동일 게시글
        events.add(new CommentCreatedEvent(1L, "댓글작성자2", 100L));  // 동일 게시글
        events.add(new CommentCreatedEvent(2L, "댓글작성자3", 101L));  // 다른 게시글

        // When & Then - 각 이벤트가 모두 처리되는지 검증
        publishEvents(events);
        verifyAsyncSlow(() -> {
            // 첫 번째 이벤트
            verify(notificationSseUseCase).sendCommentNotification(
                    eq(1L), eq("댓글작성자1"), eq(100L));
            verify(notificationFcmUseCase).sendCommentNotification(
                    eq(1L), eq("댓글작성자1"));
            
            // 두 번째 이벤트
            verify(notificationSseUseCase).sendCommentNotification(
                    eq(1L), eq("댓글작성자2"), eq(100L));
            verify(notificationFcmUseCase).sendCommentNotification(
                    eq(1L), eq("댓글작성자2"));
            
            // 세 번째 이벤트
            verify(notificationSseUseCase).sendCommentNotification(
                    eq(2L), eq("댓글작성자3"), eq(101L));
            verify(notificationFcmUseCase).sendCommentNotification(
                    eq(2L), eq("댓글작성자3"));
        });
    }
}