package jaeik.bimillog.integration.event.auth;

import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>사용자 탈퇴 이벤트 워크플로우 통합 테스트</h2>
 * <p>사용자 탈퇴 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
@DisplayName("사용자 탈퇴 이벤트 워크플로우 통합 테스트")
class UserWithdrawnEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private CommentCommandUseCase commentCommandUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;



    @Test
    @DisplayName("사용자 탈퇴 이벤트 워크플로우 - 댓글 처리 및 FCM 토큰 삭제 완료")
    void userWithdrawnEventWorkflow_ShouldCompleteAllCleanupTasks() {
        // Given
        Long userId = 1L;
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
                });
    }





    @Test
    @DisplayName("여러 사용자 탈퇴 이벤트 동시 처리")
    void multipleUserWithdrawnEvents() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // When - 여러 사용자 탈퇴 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId1));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId2));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId3));

        // Then - 모든 사용자의 댓글 처리 및 FCM 토큰 삭제가 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId1));
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId2));
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId3));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId1));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId2));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId3));
                });
    }




    @Test
    @DisplayName("이벤트 처리 시간 검증 - 사용자 탈퇴")
    void userWithdrawnEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(1L);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(1L));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("탈퇴 이벤트에서 리스너의 null 값 안전 처리")
    void userWithdrawnEventWithNullUserId_ShouldBeHandledSafely() {
        // Given - null userId를 포함한 탈퇴 이벤트 (리스너에서 안전하게 처리되어야 함)
        UserWithdrawnEvent withdrawnEvent = new UserWithdrawnEvent(null);

        // When - null userId로 이벤트 발행
        eventPublisher.publishEvent(withdrawnEvent);

        // Then - 리스너들이 null userId를 안전하게 처리해야 함 (예외 발생하지 않음)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // 리스너들은 null 값을 받을 수 있지만 안전하게 처리해야 함
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(null));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(null));
                });
    }

    @Test
    @DisplayName("대용량 이벤트 처리 - 100명 사용자 동시 탈퇴")
    void bulkUserWithdrawnEvents_ShouldCompleteAllWithinTimeout() {
        // Given - 100명의 사용자 탈퇴 이벤트
        int eventCount = 100;

        // When - 대량 탈퇴 이벤트 발행
        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new UserWithdrawnEvent((long) i));
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    // 모든 사용자에 대해 댓글 처리 및 FCM 토큰 삭제가 호출되었는지 확인
                    for (int i = 1; i <= eventCount; i++) {
                        verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq((long) i));
                        verify(notificationFcmUseCase).deleteFcmTokens(eq((long) i));
                    }
                });
    }

    @Test
    @DisplayName("이벤트 발행 순서와 처리 순서 - 비동기 특성 확인")
    void eventOrderAndAsyncProcessing() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // When - 순서대로 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId1));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId2));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId3));

        // Then - 비동기 처리이므로 순서와 관계없이 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId1));
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId2));
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(userId3));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId1));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId2));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId3));
                });
    }


    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 댓글 처리 실패")
    void eventProcessingWithException_CommentProcessingFailure() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(1L);
        
        // 댓글 처리 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 처리 실패"))
                .when(commentCommandUseCase).processUserCommentsOnWithdrawal(1L);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
                    // FCM 토큰 삭제는 별도 리스너이므로 댓글 처리 실패와 관계없이 처리되어야 함
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(1L));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - FCM 토큰 삭제 실패")
    void eventProcessingWithException_FcmTokenDeletionFailure() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(1L);
        
        // FCM 토큰 삭제 실패 시뮬레이션
        doThrow(new RuntimeException("FCM 토큰 삭제 실패"))
                .when(notificationFcmUseCase).deleteFcmTokens(1L);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(1L));
                    // 댓글 처리는 별도 리스너이므로 FCM 토큰 삭제 실패와 관계없이 처리되어야 함
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
                });
    }
}