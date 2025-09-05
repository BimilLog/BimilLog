package jaeik.bimillog.integration.event.comment;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
@DisplayName("댓글 생성 이벤트 워크플로우 통합 테스트")
class CommentCreatedEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
    @DisplayName("댓글 생성 이벤트 유효성 검사 - null 값 검증")
    void commentCreatedEventValidation_ShouldThrowExceptionForNullValues() {
        // Then - null 값들로 이벤트 생성 시 예외가 발생해야 함
        assertThatThrownBy(() -> new CommentCreatedEvent(null, "댓글러", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글 작성자 ID는 null일 수 없습니다");
        
        assertThatThrownBy(() -> new CommentCreatedEvent(1L, null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("댓글 작성자 이름은 null이거나 비어있을 수 없습니다");
        
        assertThatThrownBy(() -> new CommentCreatedEvent(1L, "", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("댓글 작성자 이름은 null이거나 비어있을 수 없습니다");
        
        assertThatThrownBy(() -> new CommentCreatedEvent(1L, "댓글러", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글 ID는 null일 수 없습니다");
    }

    @Test
    @DisplayName("여러 게시글의 댓글 생성 이벤트 처리")
    void multipleCommentCreatedEvents_ForDifferentPosts() {
        // Given - 여러 게시글에 댓글 생성
        CommentCreatedEvent event1 = new CommentCreatedEvent(1L, "댓글러1", 101L);
        CommentCreatedEvent event2 = new CommentCreatedEvent(2L, "댓글러2", 102L);
        CommentCreatedEvent event3 = new CommentCreatedEvent(3L, "댓글러3", 103L);

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 게시글의 댓글 생성 알림이 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(1L), eq("댓글러1"), eq(101L));
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(2L), eq("댓글러2"), eq(102L));
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(3L), eq("댓글러3"), eq(103L));
                    
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(1L), eq("댓글러1"));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(2L), eq("댓글러2"));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(3L), eq("댓글러3"));
                });
    }

    @Test
    @DisplayName("대량 댓글 생성 이벤트 처리")
    void massCommentCreatedEvents_ShouldProcessEfficiently() {
        // Given - 대량의 댓글 생성 이벤트 (50개)
        int eventCount = 50;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 댓글 생성 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            CommentCreatedEvent event = new CommentCreatedEvent(
                    (long) i, 
                    "댓글러" + i,
                    (long) (i + 1000));
            eventPublisher.publishEvent(event);
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    for (int i = 1; i <= eventCount; i++) {
                        verify(notificationSseUseCase).sendCommentNotification(
                                eq((long) i), eq("댓글러" + i), eq((long) (i + 1000)));
                        verify(notificationFcmUseCase).sendCommentNotification(
                                eq((long) i), eq("댓글러" + i));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 15초를 초과하지 않아야 함
                    assert totalProcessingTime < 15000L : "대량 댓글 생성 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - SSE 알림 실패")
    void eventProcessingWithException_SseNotificationFailure() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "댓글러", 100L);
        
        // SSE 알림 실패 시뮬레이션
        doThrow(new RuntimeException("SSE 알림 실패"))
                .when(notificationSseUseCase).sendCommentNotification(1L, "댓글러", 100L);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(eq(1L), eq("댓글러"), eq(100L));
                    // FCM 알림은 별도 처리이므로 SSE 실패와 관계없이 처리되어야 함
                    verify(notificationFcmUseCase).sendCommentNotification(eq(1L), eq("댓글러"));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - FCM 알림 실패")
    void eventProcessingWithException_FcmNotificationFailure() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "댓글러", 100L);
        
        // FCM 알림 실패 시뮬레이션
        doThrow(new RuntimeException("FCM 알림 실패"))
                .when(notificationFcmUseCase).sendCommentNotification(1L, "댓글러");

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationFcmUseCase).sendCommentNotification(eq(1L), eq("댓글러"));
                    // SSE 알림은 별도 처리이므로 FCM 실패와 관계없이 처리되어야 함
                    verify(notificationSseUseCase).sendCommentNotification(eq(1L), eq("댓글러"), eq(100L));
                });
    }
}