package jaeik.bimillog.event.comment;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>댓글 생성 이벤트 워크플로우 통합 테스트</h2>
 * <p>댓글 생성 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("댓글 생성 이벤트 워크플로우 통합 테스트")
class CommentCreatedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("댓글 생성 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
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
    @DisplayName("동일한 게시글의 여러 댓글 생성 이벤트")
    void multipleCommentCreatedEvents_ForSamePost() {
        // Given
        var events = new java.util.ArrayList<CommentCreatedEvent>();
        for (int i = 1; i <= 3; i++) {
            events.add(new CommentCreatedEvent(1L, "댓글작성자" + i, 100L));
        }

        // When & Then
        publishEvents(events);
        verifyAsyncSlow(() -> {
            for (int i = 1; i <= 3; i++) {
                verify(notificationSseUseCase).sendCommentNotification(
                        eq(1L), eq("댓글작성자" + i), eq(100L));
                verify(notificationFcmUseCase).sendCommentNotification(
                        eq(1L), eq("댓글작성자" + i));
            }
        });
    }


    @Test
    @DisplayName("여러 게시글의 댓글 생성 이벤트 처리")
    void multipleCommentCreatedEvents_ForDifferentPosts() {
        // Given
        var events = new java.util.ArrayList<CommentCreatedEvent>();
        for (int i = 1; i <= 3; i++) {
            events.add(new CommentCreatedEvent((long) i, "댓글러" + i, 100L + i));
        }

        // When & Then
        publishEvents(events);
        verifyAsyncSlow(() -> {
            for (int i = 1; i <= 3; i++) {
                verify(notificationSseUseCase).sendCommentNotification(
                        eq((long) i), eq("댓글러" + i), eq(100L + i));
                verify(notificationFcmUseCase).sendCommentNotification(
                        eq((long) i), eq("댓글러" + i));
            }
        });
    }


    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - SSE 알림 실패")
    void eventProcessingWithException_SseNotificationFailure() {
        // Given
        var event = new CommentCreatedEvent(1L, "댓글러", 100L);

        // SSE 알림 실패 시뮬레이션
        doThrow(new RuntimeException("SSE 알림 실패"))
                .when(notificationSseUseCase).sendCommentNotification(1L, "댓글러", 100L);

        // When & Then
        publishAndExpectException(event, () -> {
            verify(notificationSseUseCase).sendCommentNotification(eq(1L), eq("댓글러"), eq(100L));
            // FCM 알림은 별도 처리이므로 SSE 실패와 관계없이 처리되어야 함
            verify(notificationFcmUseCase).sendCommentNotification(eq(1L), eq("댓글러"));
        });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - FCM 알림 실패")
    void eventProcessingWithException_FcmNotificationFailure() {
        // Given
        var event = new CommentCreatedEvent(1L, "댓글러", 100L);

        // FCM 알림 실패 시뮬레이션
        doThrow(new RuntimeException("FCM 알림 실패"))
                .when(notificationFcmUseCase).sendCommentNotification(1L, "댓글러");

        // When & Then
        publishAndExpectException(event, () -> {
            verify(notificationFcmUseCase).sendCommentNotification(eq(1L), eq("댓글러"));
            // SSE 알림은 별도 처리이므로 FCM 실패와 관계없이 처리되어야 함
            verify(notificationSseUseCase).sendCommentNotification(eq(1L), eq("댓글러"), eq(100L));
        });
    }
}