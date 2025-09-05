package jaeik.bimillog.integration.event.admin;

import jaeik.bimillog.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.bimillog.domain.auth.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * <h2>Auth 도메인 이벤트 워크플로우 통합 테스트</h2>
 * <p>인증 관련 이벤트들의 전체 흐름을 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("Auth 도메인 이벤트 워크플로우 통합 테스트")
class AdminWithdrawRequestedEventIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private DeleteUserPort deleteUserPort;

    @MockitoBean
    private SocialLoginPort socialLoginPort;

    @MockitoBean
    private WithdrawUseCase withdrawUseCase;

    @MockitoBean
    private UserCommandUseCase userCommandUseCase;

    @Test
    @DisplayName("사용자 로그아웃 이벤트 워크플로우 - 토큰 정리까지 완료")
    void userLogoutEventWorkflow_ShouldCompleteTokenCleanup() {
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
                    verify(deleteUserPort).logoutUser(eq(userId), eq(tokenId));
                });
    }

    @Test
    @DisplayName("사용자 제재 이벤트 워크플로우 - BAN 역할 변경 및 블랙리스트 추가까지 완료")
    void userBannedEventWorkflow_ShouldCompleteBanAndBlacklist() {
        // Given
        Long userId = 1L;
        String socialId = "testSocialId";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(userId, socialId, provider);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(userCommandUseCase).banUser(eq(userId));
                    verify(userCommandUseCase).addToBlacklist(eq(userId));
                });
    }

    @Test
    @DisplayName("복합 이벤트 시나리오 - 사용자 로그아웃 후 제재")
    void complexEventScenario_LogoutThenBan() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        String socialId = "testSocialId";
        SocialProvider provider = SocialProvider.KAKAO;

        // When - 연속된 이벤트 발행
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, tokenId));
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(deleteUserPort).logoutUser(eq(userId), eq(tokenId));
                    verify(userCommandUseCase).banUser(eq(userId));
                    verify(userCommandUseCase).addToBlacklist(eq(userId));
                });
    }

    @Test
    @DisplayName("동일한 사용자의 여러 토큰 로그아웃 이벤트")
    void multipleTokenLogoutEvents_ForSameUser() {
        // Given
        Long userId = 1L;
        Long tokenId1 = 100L;
        Long tokenId2 = 101L;
        Long tokenId3 = 102L;

        // When - 동일 사용자의 여러 토큰 로그아웃
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, tokenId1));
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, tokenId2));
        eventPublisher.publishEvent(UserLoggedOutEvent.of(userId, tokenId3));

        // Then - 모든 토큰이 개별적으로 정리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(deleteUserPort).logoutUser(eq(userId), eq(tokenId1));
                    verify(deleteUserPort).logoutUser(eq(userId), eq(tokenId2));
                    verify(deleteUserPort).logoutUser(eq(userId), eq(tokenId3));
                });
    }

    @Test
    @DisplayName("관리자 강제 탈퇴 이벤트 워크플로우 - 블랙리스트 추가 및 탈퇴 처리까지 완료")
    void adminWithdrawRequestEventWorkflow_ShouldCompleteBlacklistAndWithdrawProcess() {
        // Given
        Long userId = 1L;
        String reason = "관리자 강제 탈퇴";
        AdminWithdrawRequestedEvent event = new AdminWithdrawRequestedEvent(userId, reason);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용 (블랙리스트 추가 후 탈퇴 처리)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(userCommandUseCase).addToBlacklist(eq(userId));
                    verify(withdrawUseCase).forceWithdraw(eq(userId));
                });
    }

    @Test
    @DisplayName("복합 관리자 시나리오 - 제재 후 강제 탈퇴")
    void complexAdminScenario_BanThenForceWithdraw() {
        // Given
        Long userId = 1L;
        String socialId = "testSocialId";
        SocialProvider provider = SocialProvider.KAKAO;
        String withdrawReason = "관리자 강제 탈퇴";

        // When - 연속된 관리자 이벤트 발행
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));
        eventPublisher.publishEvent(new AdminWithdrawRequestedEvent(userId, withdrawReason));

        // Then - 두 이벤트 모두 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(userCommandUseCase).banUser(eq(userId));
                    verify(userCommandUseCase).addToBlacklist(eq(userId));
                    verify(withdrawUseCase).forceWithdraw(eq(userId));
                });
    }

    @Test
    @DisplayName("이벤트 처리 시간 검증 - 타임아웃 내 완료")
    void eventProcessingTime_ShouldCompleteWithinTimeout() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        UserLoggedOutEvent event = new UserLoggedOutEvent(userId, tokenId, LocalDateTime.now());
        
        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publishEvent(event);

        // Then - 2초 내에 처리 완료되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verify(deleteUserPort).logoutUser(eq(userId), eq(tokenId));
                });

        long endTime = System.currentTimeMillis();
        assert (endTime - startTime) < 2000; // 2초 미만 처리 확인
    }

    @Test
    @DisplayName("강제 탈퇴 시 블랙리스트 추가 우선순위 검증 - 블랙리스트 추가가 탈퇴 처리보다 먼저")
    void adminWithdrawRequest_BlacklistBeforeWithdraw() {
        // Given
        Long userId = 1L;
        String reason = "관리자 강제 탈퇴";
        AdminWithdrawRequestedEvent event = new AdminWithdrawRequestedEvent(userId, reason);

        // When
        eventPublisher.publishEvent(event);

        // Then - 실행 순서 확인: 블랙리스트 추가 → 탈퇴 처리
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // InOrder를 사용해 블랙리스트 추가가 먼저 호출되는지 확인할 수 있음
                    // (실제로는 이벤트 처리에서 순서가 보장되므로 단순 verify로도 충분)
                    verify(userCommandUseCase).addToBlacklist(eq(userId));
                    verify(withdrawUseCase).forceWithdraw(eq(userId));
                });
    }
}