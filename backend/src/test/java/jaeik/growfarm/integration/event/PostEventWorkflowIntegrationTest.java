package jaeik.growfarm.integration.event;

import jaeik.growfarm.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.growfarm.domain.post.event.PostDeletedEvent;
import jaeik.growfarm.domain.post.event.PostFeaturedEvent;
import jaeik.growfarm.domain.post.event.PostSetAsNoticeEvent;
import jaeik.growfarm.domain.post.event.PostUnsetAsNoticeEvent;
import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.domain.notification.application.port.in.NotificationSseUseCase;
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
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * <h2>게시글 도메인 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 관련 이벤트들의 전체 흐름을 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("게시글 도메인 이벤트 워크플로우 통합 테스트")
class PostEventWorkflowIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private PostLikeCommandPort postLikeCommandPort;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("게시글 삭제 이벤트 워크플로우 - 추천 삭제까지 완료")
    void postDeletedEventWorkflow_ShouldCompletePostLikeDeletion() {
        // Given
        Long postId = 100L;
        String postTitle = "테스트 게시글";
        PostDeletedEvent event = new PostDeletedEvent(postId, postTitle);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId));
                });
    }

    @Test
    @DisplayName("게시글 인기글 등극 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void postFeaturedEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long userId = 1L;
        Long postId = 100L;
        String sseMessage = "축하합니다! 회원님의 게시글이 주간 인기글에 선정되었습니다!";
        String fcmTitle = "🎉 인기글 등극!";
        String fcmBody = "회원님의 게시글이 주간 인기글에 선정되었습니다!";
        
        PostFeaturedEvent event = new PostFeaturedEvent(
                this, userId, sseMessage, postId, fcmTitle, fcmBody);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq(sseMessage), eq(postId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq(fcmTitle), eq(fcmBody));
                });
    }

    @Test
    @DisplayName("게시글 공지 설정 이벤트 - 현재는 리스너가 없어서 처리되지 않음")
    void postSetAsNoticeEvent_NoListenerExists() {
        // Given
        Long postId = 100L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 현재 이 이벤트에 대한 리스너가 없으므로 상호작용 없음
        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verifyNoInteractions(postLikeCommandPort);
                    verifyNoInteractions(notificationSseUseCase);
                    verifyNoInteractions(notificationFcmUseCase);
                });
    }

    @Test
    @DisplayName("게시글 공지 해제 이벤트 - 현재는 리스너가 없어서 처리되지 않음")
    void postUnsetAsNoticeEvent_NoListenerExists() {
        // Given
        Long postId = 100L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 현재 이 이벤트에 대한 리스너가 없으므로 상호작용 없음
        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verifyNoInteractions(postLikeCommandPort);
                    verifyNoInteractions(notificationSseUseCase);
                    verifyNoInteractions(notificationFcmUseCase);
                });
    }

    @Test
    @DisplayName("복합 이벤트 시나리오 - 게시글 인기글 등극 후 삭제")
    void complexEventScenario_PostFeaturedThenDeleted() {
        // Given
        Long userId = 1L;
        Long postId = 100L;
        String sseMessage = "인기글 등극!";
        String fcmTitle = "축하!";
        String fcmBody = "인기글이 되었습니다!";

        // When - 연속된 이벤트 발행
        eventPublisher.publishEvent(new PostFeaturedEvent(
                this, userId, sseMessage, postId, fcmTitle, fcmBody));
        eventPublisher.publishEvent(new PostDeletedEvent(postId, "테스트 게시글"));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 인기글 등극 알림
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq(sseMessage), eq(postId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq(fcmTitle), eq(fcmBody));
                    
                    // 게시글 삭제로 인한 추천 삭제
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId));
                });
    }

    @Test
    @DisplayName("여러 게시글 삭제 이벤트 동시 처리")
    void multiplePostDeletionEvents() {
        // Given
        Long postId1 = 100L;
        Long postId2 = 200L;
        Long postId3 = 300L;

        // When - 여러 게시글 삭제 이벤트 발행
        eventPublisher.publishEvent(new PostDeletedEvent(postId1, "테스트 게시글1"));
        eventPublisher.publishEvent(new PostDeletedEvent(postId2, "테스트 게시글2"));
        eventPublisher.publishEvent(new PostDeletedEvent(postId3, "테스트 게시글3"));

        // Then - 모든 게시글의 추천이 삭제되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId1));
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId2));
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId3));
                });
    }

    @Test
    @DisplayName("여러 인기글 등극 이벤트 동시 처리")
    void multiplePostFeaturedEvents() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long postId1 = 100L;
        Long postId2 = 200L;
        
        PostFeaturedEvent event1 = new PostFeaturedEvent(
                this, userId1, "사용자1 인기글!", postId1, "축하1", "인기글1");
        PostFeaturedEvent event2 = new PostFeaturedEvent(
                this, userId2, "사용자2 인기글!", postId2, "축하2", "인기글2");

        // When - 여러 인기글 등극 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);

        // Then - 모든 사용자에게 알림이 전송되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId1), eq("사용자1 인기글!"), eq(postId1));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId1), eq("축하1"), eq("인기글1"));
                    
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId2), eq("사용자2 인기글!"), eq(postId2));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId2), eq("축하2"), eq("인기글2"));
                });
    }

    @Test
    @DisplayName("동일 게시글에 대한 여러 이벤트 처리")
    void multipleEventsForSamePost() {
        // Given
        Long postId = 100L;
        Long userId = 1L;

        // When - 동일 게시글에 대해 인기글 등극과 삭제 이벤트 연속 발행
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId));
        eventPublisher.publishEvent(new PostFeaturedEvent(
                this, userId, "인기글!", postId, "축하", "등극"));
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));
        eventPublisher.publishEvent(new PostDeletedEvent(postId, "테스트 게시글"));

        // Then - 처리 가능한 이벤트들만 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 인기글 등극 알림
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("인기글!"), eq(postId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("축하"), eq("등극"));
                    
                    // 게시글 삭제로 인한 추천 삭제
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId));
                });
    }

    @Test
    @DisplayName("이벤트 처리 시간 검증 - 게시글 삭제")
    void postDeletedEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long postId = 100L;
        PostDeletedEvent event = new PostDeletedEvent(postId, "성능 테스트 게시글");

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("이벤트 처리 시간 검증 - 인기글 등극")
    void postFeaturedEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long userId = 1L;
        Long postId = 100L;
        PostFeaturedEvent event = new PostFeaturedEvent(
                this, userId, "성능테스트", postId, "제목", "내용");

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 5초 내에 처리 완료되어야 함 (FCM은 더 오래 걸릴 수 있음)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("성능테스트"), eq(postId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("제목"), eq("내용"));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 5초를 초과하지 않아야 함
                    assert processingTime < 5000L : "이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("null 값을 포함한 게시글 이벤트 처리")
    void postEventsWithNullValues_ShouldBeProcessed() {
        // Given - null 값들을 포함한 이벤트들
        PostDeletedEvent deleteEvent = new PostDeletedEvent(null, null);
        PostFeaturedEvent featuredEvent = new PostFeaturedEvent(
                this, null, null, null, null, null);
        PostSetAsNoticeEvent setNoticeEvent = new PostSetAsNoticeEvent(null);
        PostUnsetAsNoticeEvent unsetNoticeEvent = new PostUnsetAsNoticeEvent(null);

        // When
        eventPublisher.publishEvent(deleteEvent);
        eventPublisher.publishEvent(featuredEvent);
        eventPublisher.publishEvent(setNoticeEvent);
        eventPublisher.publishEvent(unsetNoticeEvent);

        // Then - null 값이어도 처리 가능한 이벤트들은 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 게시글 삭제 이벤트는 처리됨
                    verify(postLikeCommandPort).deleteAllByPostId(eq(null));
                    
                    // 인기글 등극 이벤트도 처리됨 (null 값이라도)
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(null), eq(null), eq(null));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(null), eq(null), eq(null));
                });
    }

    @Test
    @DisplayName("대용량 이벤트 처리 - 100개 게시글 동시 삭제")
    void bulkPostDeletionEvents_ShouldCompleteAllWithinTimeout() {
        // Given - 100개의 게시글 삭제 이벤트
        int eventCount = 100;

        // When - 대량 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new PostDeletedEvent((long) i, "대량테스트" + i));
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    // 모든 게시글 ID에 대해 삭제가 호출되었는지 확인
                    for (int i = 1; i <= eventCount; i++) {
                        verify(postLikeCommandPort).deleteAllByPostId(eq((long) i));
                    }
                });
    }

    @Test
    @DisplayName("이벤트 발행 순서와 처리 순서 - 비동기 특성 확인")
    void eventOrderAndAsyncProcessing() {
        // Given
        Long postId1 = 1L;
        Long postId2 = 2L;
        Long postId3 = 3L;

        // When - 순서대로 이벤트 발행
        eventPublisher.publishEvent(new PostDeletedEvent(postId1, "첫번째"));
        eventPublisher.publishEvent(new PostDeletedEvent(postId2, "두번째"));
        eventPublisher.publishEvent(new PostDeletedEvent(postId3, "세번째"));

        // Then - 비동기 처리이므로 순서와 관계없이 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId1));
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId2));
                    verify(postLikeCommandPort).deleteAllByPostId(eq(postId3));
                });
    }
}