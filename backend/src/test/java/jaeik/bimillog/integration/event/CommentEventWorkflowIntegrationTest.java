package jaeik.bimillog.integration.event;

import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.port.out.CommentCommandPort;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.post.event.PostDeletedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * <h2>댓글 도메인 이벤트 워크플로우 통합 테스트</h2>
 * <p>댓글 관련 이벤트들의 전체 흐름을 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("댓글 도메인 이벤트 워크플로우 통합 테스트")
class CommentEventWorkflowIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private CommentCommandPort commentCommandPort;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("댓글 생성 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void commentCreatedEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long postUserId = 1L;
        String commenterName = "댓글작성자";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(postUserId, commenterName, postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenterName), eq(postId));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenterName));
                });
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 워크플로우 - 댓글 익명화까지 완료")
    void userWithdrawnEventWorkflow_ShouldCompleteCommentAnonymization() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(userId));
                });
    }

    @Test
    @DisplayName("게시글 삭제 이벤트 워크플로우 - 댓글 삭제까지 완료")
    void postDeletedEventWorkflow_ShouldCompleteCommentDeletion() {
        // Given
        Long postId = 100L;
        PostDeletedEvent event = new PostDeletedEvent(postId, "테스트 게시글");

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(commentCommandPort).deleteAllByPostId(eq(postId));
                });
    }

    @Test
    @DisplayName("복합 이벤트 시나리오 - 댓글 생성 후 게시글 삭제")
    void complexEventScenario_CommentCreatedThenPostDeleted() {
        // Given
        Long postUserId = 1L;
        String commenterName = "댓글작성자";
        Long postId = 100L;

        // When - 연속된 이벤트 발행
        eventPublisher.publishEvent(new CommentCreatedEvent(postUserId, commenterName, postId));
        eventPublisher.publishEvent(new PostDeletedEvent(postId, "테스트 게시글"));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 댓글 생성 알림
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenterName), eq(postId));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenterName));
                    
                    // 게시글 삭제로 인한 댓글 삭제
                    verify(commentCommandPort).deleteAllByPostId(eq(postId));
                });
    }

    @Test
    @DisplayName("동일한 게시글의 여러 댓글 생성 이벤트")
    void multipleCommentCreatedEvents_ForSamePost() {
        // Given
        Long postUserId = 1L;
        Long postId = 100L;
        String commenter1 = "댓글작성자1";
        String commenter2 = "댓글작성자2";
        String commenter3 = "댓글작성자3";

        // When - 동일 게시글에 여러 댓글 생성
        eventPublisher.publishEvent(new CommentCreatedEvent(postUserId, commenter1, postId));
        eventPublisher.publishEvent(new CommentCreatedEvent(postUserId, commenter2, postId));
        eventPublisher.publishEvent(new CommentCreatedEvent(postUserId, commenter3, postId));

        // Then - 모든 댓글이 개별적으로 알림 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenter1), eq(postId));
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenter2), eq(postId));
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenter3), eq(postId));
                    
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenter1));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenter2));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenter3));
                });
    }

    @Test
    @DisplayName("사용자 탈퇴와 게시글 삭제 동시 발생")
    void simultaneousUserWithdrawalAndPostDeletion() {
        // Given
        Long userId = 1L;
        Long postId = 100L;

        // When - 동시에 사용자 탈퇴와 게시글 삭제 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
        eventPublisher.publishEvent(new PostDeletedEvent(postId, "테스트 게시글"));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(userId));
                    verify(commentCommandPort).deleteAllByPostId(eq(postId));
                });
    }

    @Test
    @DisplayName("여러 사용자 탈퇴 이벤트 처리")
    void multipleUserWithdrawalEvents() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // When - 여러 사용자 탈퇴 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId1));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId2));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId3));

        // Then - 모든 사용자의 댓글이 익명화되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(commentCommandPort).anonymizeUserComments(eq(userId1));
                    verify(commentCommandPort).anonymizeUserComments(eq(userId2));
                    verify(commentCommandPort).anonymizeUserComments(eq(userId3));
                });
    }

    @Test
    @DisplayName("여러 게시글 삭제 이벤트 처리")
    void multiplePostDeletionEvents() {
        // Given
        Long postId1 = 100L;
        Long postId2 = 200L;
        Long postId3 = 300L;

        // When - 여러 게시글 삭제 이벤트 발행
        eventPublisher.publishEvent(new PostDeletedEvent(postId1, "테스트 게시글1"));
        eventPublisher.publishEvent(new PostDeletedEvent(postId2, "테스트 게시글2"));
        eventPublisher.publishEvent(new PostDeletedEvent(postId3, "테스트 게시글3"));

        // Then - 모든 게시글의 댓글이 삭제되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(commentCommandPort).deleteAllByPostId(eq(postId1));
                    verify(commentCommandPort).deleteAllByPostId(eq(postId2));
                    verify(commentCommandPort).deleteAllByPostId(eq(postId3));
                });
    }

    @Test
    @DisplayName("이벤트 처리 시간 검증 - 댓글 생성 알림")
    void commentCreatedEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long postUserId = 1L;
        String commenterName = "테스트댓글러";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(postUserId, commenterName, postId);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenterName), eq(postId));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(postUserId), eq(commenterName));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("null 값을 포함한 이벤트 처리")
    void eventsWithNullValues_ShouldBeProcessed() {
        // Given - null 값들을 포함한 이벤트들
        CommentCreatedEvent commentEvent = new CommentCreatedEvent(1L, "테스트", 1L);
        UserWithdrawnEvent userEvent = new UserWithdrawnEvent(null);
        PostDeletedEvent postEvent = new PostDeletedEvent(null, null);

        // When
        eventPublisher.publishEvent(commentEvent);
        eventPublisher.publishEvent(userEvent);
        eventPublisher.publishEvent(postEvent);

        // Then - null 값이어도 이벤트는 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(null), eq(null), eq(null));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(null), eq(null));
                    verify(commentCommandPort).anonymizeUserComments(eq(null));
                    verify(commentCommandPort).deleteAllByPostId(eq(null));
                });
    }
}