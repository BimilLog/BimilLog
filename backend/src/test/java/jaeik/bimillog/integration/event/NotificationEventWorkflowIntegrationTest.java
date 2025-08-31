package jaeik.bimillog.integration.event;

import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.notification.application.port.out.SsePort;
import jaeik.bimillog.domain.auth.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.comment.application.port.out.CommentCommandPort;
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
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

/**
 * <h2>Notification 도메인 이벤트 워크플로우 통합 테스트</h2>
 * <p>알림 관련 이벤트들의 전체 흐름을 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("Notification 도메인 이벤트 워크플로우 통합 테스트")
class NotificationEventWorkflowIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @MockitoBean
    private SsePort ssePort;

    @MockitoBean
    private DeleteUserPort deleteUserPort;

    @MockitoBean
    private CommentCommandPort commentCommandPort;

    /**
     * <h3>각 테스트 전 초기화</h3>
     * <p>Mock 객체들을 초기화하여 테스트 간 상호작용을 방지합니다.</p>
     * <p>이는 전체 테스트 클래스 실행 시 발생할 수 있는 Mock 상태 공유 문제를 해결합니다.</p>
     * 
     * @author Jaeik
     * @since 2.0.0
     */
    @BeforeEach
    void setUp() {
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // Mock 검증 실패: 여러 테스트 실행 시 Mock 상태가 누적되어 검증 실패
        // 의심: 1) Mock Bean 상태 공유 2) 비동기 처리 타이밍 3) 트랜잭션 롤백 타이밍
        // 대응: 각 테스트 전 Mock 초기화로 상태 격리
        reset(notificationSseUseCase, notificationFcmUseCase, ssePort, deleteUserPort, commentCommandPort);
    }

    @Test
    @DisplayName("댓글 생성 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void commentCreatedEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long postUserId = 1L;
        String commenterName = "댓글작성자";
        Long postId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(this, postUserId, commenterName, postId);

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
    @DisplayName("롤링페이퍼 메시지 수신 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void rollingPaperMessageEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long paperOwnerId = 1L;
        String userName = "메시지작성자";
        RollingPaperEvent event = new RollingPaperEvent(this, paperOwnerId, userName);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendPaperPlantNotification(
                            eq(paperOwnerId), eq(userName));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(
                            eq(paperOwnerId));
                });
    }

    @Test
    @DisplayName("인기 게시글 선정 이벤트 워크플로우 - SSE와 FCM 알림까지 완료")
    void postFeaturedEventWorkflow_ShouldCompleteNotifications() {
        // Given
        Long userId = 1L;
        String sseMessage = "축하합니다! 회원님의 게시글이 인기글로 선정되었습니다.";
        Long postId = 100L;
        String fcmTitle = "인기글 선정";
        String fcmBody = "회원님의 게시글이 인기글로 선정되었습니다!";
        PostFeaturedEvent event = new PostFeaturedEvent(this, userId, sseMessage, postId, fcmTitle, fcmBody);

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
    @DisplayName("사용자 로그아웃 이벤트 워크플로우 - SSE 정리와 FCM 토큰 삭제까지 완료")
    void userLoggedOutEventWorkflow_ShouldCompleteCleanupAndTokenDeletion() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
                });
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 워크플로우 - FCM 토큰 삭제까지 완료")
    void userWithdrawnEventWorkflow_ShouldCompleteFcmTokenDeletion() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
                });
    }

    @Test
    @DisplayName("복합 이벤트 시나리오 - 댓글 생성 후 사용자 로그아웃")
    void complexEventScenario_CommentCreatedThenUserLogout() {
        // Given
        Long userId = 1L;
        String commenterName = "댓글작성자";
        Long postId = 100L;
        Long tokenId = 100L;

        // When - 연속된 이벤트 발행
        eventPublisher.publishEvent(new CommentCreatedEvent(this, userId, commenterName, postId));
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, tokenId));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 댓글 생성 알림
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(userId), eq(commenterName), eq(postId));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(userId), eq(commenterName));
                    
                    // 사용자 로그아웃 후 정리
                    verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
                });
    }

    @Test
    @DisplayName("여러 알림 이벤트 동시 처리")
    void multipleNotificationEvents_ShouldProcessAllEvents() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        String commenterName1 = "댓글작성자1";
        String userName2 = "메시지작성자2";
        Long postId = 100L;

        // When - 다양한 알림 이벤트 동시 발행
        eventPublisher.publishEvent(new CommentCreatedEvent(this, userId1, commenterName1, postId));
        eventPublisher.publishEvent(new RollingPaperEvent(this, userId2, userName2));
        eventPublisher.publishEvent(new PostFeaturedEvent(this, userId1, "인기글 메시지", postId, "인기글", "축하합니다"));

        // Then - 모든 이벤트가 개별적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 댓글 생성 알림
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(userId1), eq(commenterName1), eq(postId));
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(userId1), eq(commenterName1));
                    
                    // 롤링페이퍼 메시지 알림
                    verify(notificationSseUseCase).sendPaperPlantNotification(
                            eq(userId2), eq(userName2));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(
                            eq(userId2));
                    
                    // 인기글 선정 알림
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId1), eq("인기글 메시지"), eq(postId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId1), eq("인기글"), eq("축하합니다"));
                });
    }

    @Test
    @DisplayName("동일한 사용자의 여러 알림 이벤트")
    void multipleNotificationEventsForSameUser() {
        // Given
        Long userId = 1L;
        String commenterName = "댓글작성자";
        String userName = "메시지작성자";
        Long postId1 = 100L;
        Long postId2 = 200L;

        // When - 동일 사용자에 대한 여러 알림 이벤트
        eventPublisher.publishEvent(new CommentCreatedEvent(this, userId, commenterName, postId1));
        eventPublisher.publishEvent(new RollingPaperEvent(this, userId, userName));
        eventPublisher.publishEvent(new PostFeaturedEvent(this, userId, "인기글 메시지", postId2, "인기글", "축하합니다"));

        // Then - 모든 알림이 해당 사용자에게 전송되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(
                            eq(userId), eq(commenterName), eq(postId1));
                    verify(notificationSseUseCase).sendPaperPlantNotification(
                            eq(userId), eq(userName));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("인기글 메시지"), eq(postId2));
                    
                    verify(notificationFcmUseCase).sendCommentNotification(
                            eq(userId), eq(commenterName));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(
                            eq(userId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(
                            eq(userId), eq("인기글"), eq("축하합니다"));
                });
    }

    @Test
    @DisplayName("사용자 로그아웃 후 탈퇴 시나리오")
    void userLogoutThenWithdrawalScenario() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;

        // When - 로그아웃 후 탈퇴 이벤트 발행
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, tokenId));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 로그아웃 처리 (SSE 정리 + FCM 토큰 삭제)
                    verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
                    
                    // FCM 토큰 삭제가 두 번 호출됨 (로그아웃과 탈퇴에서 각각)
                    verify(notificationFcmUseCase, org.mockito.Mockito.times(2)).deleteFcmTokens(eq(userId));
                });
    }

    @Test
    @DisplayName("대량 알림 이벤트 처리 성능 테스트")
    void bulkNotificationEvents_ShouldProcessWithinReasonableTime() {
        // Given
        int eventCount = 100;
        long startTime = System.currentTimeMillis();

        // When - 대량 댓글 생성 이벤트 발행
        for (int i = 0; i < eventCount; i++) {
            Long userId = (long) (i % 10); // 10명의 사용자에게 분산
            eventPublisher.publishEvent(new CommentCreatedEvent(this, userId, "댓글작성자" + i, (long) (i + 100)));
        }

        // Then - 모든 이벤트가 10초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // 모든 댓글 생성 알림이 처리되었는지 확인
                    verify(notificationSseUseCase, org.mockito.Mockito.times(eventCount))
                            .sendCommentNotification(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
                    verify(notificationFcmUseCase, org.mockito.Mockito.times(eventCount))
                            .sendCommentNotification(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
                });

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // 처리 시간이 10초를 초과하지 않아야 함
        assert processingTime < 10000L : "대량 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
    }

    @Test
    @DisplayName("null 값을 포함한 알림 이벤트 처리")
    void notificationEventsWithNullValues_ShouldBeProcessed() {
        // Given - null 값들을 포함한 이벤트들
        CommentCreatedEvent commentEvent = new CommentCreatedEvent(this, null, null, null);
        RollingPaperEvent paperEvent = new RollingPaperEvent(this, null, null);
        PostFeaturedEvent postEvent = new PostFeaturedEvent(this, null, null, null, null, null);

        // When
        eventPublisher.publishEvent(commentEvent);
        eventPublisher.publishEvent(paperEvent);
        eventPublisher.publishEvent(postEvent);

        // Then - null 값이어도 이벤트는 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(eq(null), eq(null), eq(null));
                    verify(notificationFcmUseCase).sendCommentNotification(eq(null), eq(null));
                    
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(null), eq(null));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(null));
                    
                    verify(notificationSseUseCase).sendPostFeaturedNotification(eq(null), eq(null), eq(null));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(eq(null), eq(null), eq(null));
                });
    }

    @Test
    @DisplayName("이벤트 처리 시간 검증 - 각 알림 타입별")
    void eventProcessingTime_ForEachNotificationType() {
        // Given
        Long userId = 1L;
        long startTime = System.currentTimeMillis();

        // When - 각각 다른 타입의 알림 이벤트 발행
        eventPublisher.publishEvent(new CommentCreatedEvent(this, userId, "댓글러", 100L));
        eventPublisher.publishEvent(new RollingPaperEvent(this, userId, "메시지작성자"));
        eventPublisher.publishEvent(new PostFeaturedEvent(this, userId, "축하", 100L, "제목", "내용"));

        // Then - 3초 내에 모든 알림이 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(notificationSseUseCase).sendCommentNotification(eq(userId), eq("댓글러"), eq(100L));
                    verify(notificationSseUseCase).sendPaperPlantNotification(eq(userId), eq("메시지작성자"));
                    verify(notificationSseUseCase).sendPostFeaturedNotification(eq(userId), eq("축하"), eq(100L));
                    
                    verify(notificationFcmUseCase).sendCommentNotification(eq(userId), eq("댓글러"));
                    verify(notificationFcmUseCase).sendPaperPlantNotification(eq(userId));
                    verify(notificationFcmUseCase).sendPostFeaturedNotification(eq(userId), eq("제목"), eq("내용"));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "알림 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("SSE 연결 정리 전용 테스트")
    void sseEmitterCleanupEvents_ShouldProcessCorrectly() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // When - 여러 사용자 로그아웃 이벤트 발행
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId1, 100L));
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId2, 101L));
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId3, 102L));

        // Then - 각 사용자의 특정 토큰에 해당하는 SSE 연결만 정리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId1), eq(100L));
                    verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId2), eq(101L));
                    verify(ssePort).deleteEmitterByUserIdAndTokenId(eq(userId3), eq(102L));
                });
    }

    @Test
    @DisplayName("FCM 토큰 삭제 전용 테스트")
    void fcmTokenDeletionEvents_ShouldProcessCorrectly() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;

        // When - 로그아웃과 탈퇴 이벤트 발행
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId1, 100L));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId2));

        // Then - 모든 사용자의 FCM 토큰이 삭제되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId1));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId2));
                });
    }
}