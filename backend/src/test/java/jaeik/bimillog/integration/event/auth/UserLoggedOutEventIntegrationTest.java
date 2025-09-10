package jaeik.bimillog.integration.event.auth;

import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * <h2>사용자 로그아웃 이벤트 워크플로우 통합 테스트</h2>
 * <p>사용자 로그아웃 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
@DisplayName("사용자 로그아웃 이벤트 워크플로우 통합 테스트")
public class UserLoggedOutEventIntegrationTest {


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private WithdrawUseCase withdrawUseCase;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

    @MockitoBean
    private NotificationFcmUseCase notificationFcmUseCase;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 워크플로우 - 토큰 정리와 SSE 정리까지 완료")
    void userLoggedOutEventWorkflow_ShouldCompleteCleanupTasks() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        LocalDateTime loggedOutAt = LocalDateTime.now();
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, tokenId, loggedOutAt);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // 특정 토큰 정리
                    verify(withdrawUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
                    // 특정 기기의 SSE 연결 정리
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
                    // FCM 토큰 삭제
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(userId));
                });
    }


    @Test
    @DisplayName("여러 사용자 로그아웃 이벤트 동시 처리")
    void multipleUserLoggedOutEvents_ShouldProcessConcurrently() {
        // Given
        UserLoggedOutEvent event1 = UserLoggedOutEvent.of(1L, 101L);
        UserLoggedOutEvent event2 = UserLoggedOutEvent.of(2L, 102L);
        UserLoggedOutEvent event3 = UserLoggedOutEvent.of(3L, 103L);

        // When - 동시에 여러 로그아웃 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 사용자의 토큰과 SSE가 정리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(withdrawUseCase).cleanupSpecificToken(eq(1L), eq(101L));
                    verify(withdrawUseCase).cleanupSpecificToken(eq(2L), eq(102L));
                    verify(withdrawUseCase).cleanupSpecificToken(eq(3L), eq(103L));
                    
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(1L), eq(101L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(2L), eq(102L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(3L), eq(103L));
                    
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(1L));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(2L));
                    verify(notificationFcmUseCase).deleteFcmTokens(eq(3L));
                });
    }

    @Test
    @DisplayName("동일 사용자의 여러 로그아웃 이벤트 처리")
    void multipleLogoutEventsForSameUser_ShouldProcessAll() {
        // Given - 동일 사용자의 여러 로그아웃 (여러 기기에서 로그아웃)
        Long userId = 1L;
        UserLoggedOutEvent event1 = UserLoggedOutEvent.of(userId, 101L);
        UserLoggedOutEvent event2 = UserLoggedOutEvent.of(userId, 102L);
        UserLoggedOutEvent event3 = UserLoggedOutEvent.of(userId, 103L);

        // When
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 토큰이 개별적으로 정리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(withdrawUseCase).cleanupSpecificToken(eq(userId), eq(101L));
                    verify(withdrawUseCase).cleanupSpecificToken(eq(userId), eq(102L));
                    verify(withdrawUseCase).cleanupSpecificToken(eq(userId), eq(103L));
                    
                    // SSE는 기기별로 3번 호출됨
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(101L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(102L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(103L));
                    
                    // FCM 토큰 삭제는 사용자별로 3번 호출됨
                    verify(notificationFcmUseCase, times(3)).deleteFcmTokens(eq(userId));
                });
    }
}
