package jaeik.bimillog.integration.event.auth;

import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
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
        UserWithdrawnEvent event = new UserWithdrawnEvent(userId, "testSocialId", SocialProvider.KAKAO);

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
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId1, "testSocialId1", SocialProvider.KAKAO));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId2, "testSocialId2", SocialProvider.KAKAO));
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId3, "testSocialId3", SocialProvider.KAKAO));

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
    @DisplayName("예외 상황에서의 이벤트 처리 - 리스너들이 독립적으로 처리")
    void eventProcessingWithException_ListenersProcessIndependently() {
        // Given
        UserWithdrawnEvent event = new UserWithdrawnEvent(1L, "testSocialId", SocialProvider.KAKAO);
        
        // 댓글 처리 실패 시뮬레이션
        doThrow(new RuntimeException("댓글 처리 실패"))
                .when(commentCommandUseCase).processUserCommentsOnWithdrawal(1L);

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 리스너들이 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(commentCommandUseCase).processUserCommentsOnWithdrawal(eq(1L));
                    // FCM 토큰 삭제는 별도 리스너이므로 댓글 처리 실패와 관계없이 처리되어야 함
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(1L));
                });
    }
}