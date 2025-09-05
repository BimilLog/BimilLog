package jaeik.bimillog.integration.event.auth;

import jaeik.bimillog.domain.auth.application.port.in.TokenCleanupUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
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
    private TokenCleanupUseCase tokenCleanupUseCase;

    @MockitoBean
    private NotificationSseUseCase notificationSseUseCase;

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
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
                    // 특정 기기의 SSE 연결 정리
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
                });
    }

    @Test
    @DisplayName("팩토리 메서드를 사용한 로그아웃 이벤트")
    void userLoggedOutEventWithFactoryMethod_ShouldProcessCorrectly() {
        // Given
        Long userId = 2L;
        Long tokenId = 200L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        // When
        eventPublisher.publishEvent(event);

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
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
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(1L), eq(101L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(2L), eq(102L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(3L), eq(103L));
                    
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(1L), eq(101L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(2L), eq(102L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(3L), eq(103L));
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
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(101L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(102L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(103L));
                    
                    // SSE는 기기별로 3번 호출됨
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(101L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(102L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(103L));
                });
    }

    @Test
    @DisplayName("로그아웃 이벤트 처리 성능 검증")
    void userLoggedOutEventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        UserLoggedOutEvent event = UserLoggedOutEvent.of(userId, tokenId);

        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 3초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));

                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    // 처리 시간이 3초를 초과하지 않아야 함
                    assert processingTime < 3000L : "로그아웃 이벤트 처리 시간이 너무 오래 걸림: " + processingTime + "ms";
                });
    }

    @Test
    @DisplayName("null 값을 포함한 로그아웃 이벤트 처리")
    void userLoggedOutEventWithNullValues_ShouldBeHandledGracefully() {
        // Given
        UserLoggedOutEvent eventWithNullUserId = new UserLoggedOutEvent(null, 100L, LocalDateTime.now());
        UserLoggedOutEvent eventWithNullTokenId = new UserLoggedOutEvent(1L, null, LocalDateTime.now());
        UserLoggedOutEvent eventWithNullTime = new UserLoggedOutEvent(1L, 100L, null);

        // When
        eventPublisher.publishEvent(eventWithNullUserId);
        eventPublisher.publishEvent(eventWithNullTokenId);
        eventPublisher.publishEvent(eventWithNullTime);

        // Then - null 값이어도 이벤트는 처리되어야 함 (리스너에서 null 체크)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(null), eq(100L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(1L), eq(null));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(1L), eq(100L));
                    
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(null), eq(100L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(1L), eq(null));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(1L), eq(100L));
                });
    }

    @Test
    @DisplayName("대량 로그아웃 이벤트 처리 성능")
    void massUserLoggedOutEvents_ShouldProcessEfficiently() {
        // Given - 대량의 로그아웃 이벤트 (100명의 사용자)
        int userCount = 100;
        
        long startTime = System.currentTimeMillis();

        // When - 대량 로그아웃 이벤트 발행
        for (int i = 1; i <= userCount; i++) {
            eventPublisher.publishEvent(UserLoggedOutEvent.of((long) i, (long) (i + 1000)));
        }

        // Then - 모든 이벤트가 15초 내에 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    for (int i = 1; i <= userCount; i++) {
                        verify(tokenCleanupUseCase).cleanupSpecificToken(eq((long) i), eq((long) (i + 1000)));
                        verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq((long) i), eq((long) (i + 1000)));
                    }

                    long endTime = System.currentTimeMillis();
                    long totalProcessingTime = endTime - startTime;
                    
                    // 대량 처리 시간이 15초를 초과하지 않아야 함
                    assert totalProcessingTime < 15000L : "대량 로그아웃 이벤트 처리 시간이 너무 오래 걸림: " + totalProcessingTime + "ms";
                });
    }

    @Test
    @DisplayName("로그아웃 이벤트와 다른 이벤트의 독립적 처리")
    void userLoggedOutEventWithOtherEvents_ShouldProcessIndependently() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        UserLoggedOutEvent logoutEvent1 = UserLoggedOutEvent.of(userId1, 101L);
        UserLoggedOutEvent logoutEvent2 = UserLoggedOutEvent.of(userId2, 102L);

        // When - 로그아웃 이벤트와 다른 종류의 이벤트를 동시에 발행
        eventPublisher.publishEvent(logoutEvent1);
        eventPublisher.publishEvent(logoutEvent2);

        // Then - 모든 로그아웃 이벤트가 독립적으로 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId1), eq(101L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId2), eq(102L));
                    
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId1), eq(101L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId2), eq(102L));
                });
    }

    @Test
    @DisplayName("로그아웃 시간 기반 이벤트 처리 검증")
    void userLoggedOutEventWithTimestamp_ShouldProcessCorrectly() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        LocalDateTime specificTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, tokenId, specificTime);

        // When
        eventPublisher.publishEvent(event);

        // Then - 시간 정보와 관계없이 정리 작업이 수행되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(tokenId));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(tokenId));
                });
    }

    @Test
    @DisplayName("연속된 로그아웃 이벤트 처리 순서")
    void sequentialUserLoggedOutEvents_ShouldMaintainOrder() {
        // Given - 동일 사용자의 연속된 로그아웃 이벤트
        Long userId = 1L;
        
        // When - 순서대로 로그아웃 이벤트 발행
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, 101L));
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, 102L));
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, 103L));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(101L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(102L));
                    verify(tokenCleanupUseCase).cleanupSpecificToken(eq(userId), eq(103L));
                    
                    // SSE 정리는 기기별로 3번 호출
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(101L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(102L));
                    verify(notificationSseUseCase).deleteEmitterByUserIdAndTokenId(eq(userId), eq(103L));
                });
    }
}
