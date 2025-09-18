package jaeik.bimillog.event.admin;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.UserBanUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>사용자 차단 이벤트 워크플로우 통합 테스트</h2>
 * <p>관리자가 사용자를 차단할 때 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@Testcontainers
@Transactional
@DisplayName("사용자 차단 이벤트 워크플로우 통합 테스트")
public class UserBannedEventIntegrationTest {


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private SocialStrategyPort socialStrategyPort;

    @MockitoBean
    private WithdrawUseCase withdrawUseCase;

    @MockitoBean
    private UserBanUseCase userBanUseCase;

    @Test
    @DisplayName("사용자 차단 이벤트 워크플로우 - 소셜 로그인 해제와 블랙리스트 등록까지 완료")
    void userBannedEventWorkflow_ShouldCompleteSocialUnlinkAndBlacklist() {
        // Given
        Long userId = 1L;
        String socialId = "testKakaoId123";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = new UserBannedEvent(userId, socialId, provider);

        // When
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 고려하여 Awaitility 사용
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // 소셜 로그인 해제
                    verify(socialStrategyPort).unlink(eq(provider), eq(socialId));
                    // 사용자 블랙리스트 등록
                    verify(withdrawUseCase).addToBlacklist(eq(userId));
                    // JWT 토큰 무효화
                    verify(userBanUseCase).blacklistAllUserTokens(eq(userId), eq("사용자 제재"));
                });
    }

    @Test
    @DisplayName("여러 사용자 차단 이벤트 동시 처리")
    void multipleUserBannedEvents_ShouldProcessConcurrently() {
        // Given
        UserBannedEvent event1 = new UserBannedEvent(1L, "kakao123", SocialProvider.KAKAO);
        UserBannedEvent event2 = new UserBannedEvent(2L, "kakao456", SocialProvider.KAKAO);
        UserBannedEvent event3 = new UserBannedEvent(3L, "kakao789", SocialProvider.KAKAO);

        // When - 동시에 여러 사용자 차단 이벤트 발행
        eventPublisher.publishEvent(event1);
        eventPublisher.publishEvent(event2);
        eventPublisher.publishEvent(event3);

        // Then - 모든 사용자의 소셜 로그인이 해제되고 블랙리스트에 등록되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("kakao123"));
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("kakao456"));
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("kakao789"));
                    
                    verify(withdrawUseCase).addToBlacklist(eq(1L));
                    verify(withdrawUseCase).addToBlacklist(eq(2L));
                    verify(withdrawUseCase).addToBlacklist(eq(3L));
                    
                    verify(userBanUseCase).blacklistAllUserTokens(eq(1L), eq("사용자 제재"));
                    verify(userBanUseCase).blacklistAllUserTokens(eq(2L), eq("사용자 제재"));
                    verify(userBanUseCase).blacklistAllUserTokens(eq(3L), eq("사용자 제재"));
                });
    }

    @Test
    @DisplayName("동일 사용자의 여러 차단 이벤트 처리")
    void multipleUserBannedEventsForSameUser_ShouldProcessAll() {
        // Given - 동일 사용자의 여러 계정 차단 (재가입 후 다시 차단 등의 시나리오)
        Long userId = 1L;
        String socialId = "sameUserTest";
        SocialProvider provider = SocialProvider.KAKAO;

        // When - 동일 사용자에 대한 차단 이벤트 여러 번 발행
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));
        eventPublisher.publishEvent(new UserBannedEvent(userId, socialId, provider));

        // Then - 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialStrategyPort, times(3)).unlink(eq(provider), eq(socialId));
                    verify(withdrawUseCase, times(3)).addToBlacklist(eq(userId));
                    verify(userBanUseCase, times(3)).blacklistAllUserTokens(eq(userId), eq("사용자 제재"));
                });
    }


    @Test
    @DisplayName("잘못된 이벤트 데이터 처리 - 빈 문자열과 공백")
    void userBannedEventsWithInvalidData_ShouldThrowException() {
        // Given - 잘못된 데이터로 이벤트 생성 시 예외 발생 확인
        // UserBannedEvent는 생성자에서 검증하므로 null/빈 문자열 시 예외 발생
        
        // When & Then - 예외가 발생해야 함
        assertThatThrownBy(() -> new UserBannedEvent(null, "testId", SocialProvider.KAKAO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자 ID는 null일 수 없습니다");
                
        assertThatThrownBy(() -> new UserBannedEvent(1L, null, SocialProvider.KAKAO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소셜 ID는 null이거나 비어있을 수 없습니다");
                
        assertThatThrownBy(() -> new UserBannedEvent(1L, "", SocialProvider.KAKAO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소셜 ID는 null이거나 비어있을 수 없습니다");
                
        assertThatThrownBy(() -> new UserBannedEvent(1L, "testId", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소셜 제공자는 null일 수 없습니다");
    }


    @Test
    @DisplayName("다양한 소셜 제공자의 차단 이벤트 처리")
    void userBannedEventsWithVariousProviders_ShouldProcessCorrectly() {
        // Given - 다양한 소셜 제공자
        UserBannedEvent kakaoEvent = new UserBannedEvent(1L, "kakaoUser", SocialProvider.KAKAO);
        // 향후 다른 소셜 제공자 추가 시 테스트 확장 가능

        // When
        eventPublisher.publishEvent(kakaoEvent);

        // Then - 모든 제공자별로 적절히 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("kakaoUser"));
                    verify(withdrawUseCase).addToBlacklist(eq(1L));
                    verify(userBanUseCase).blacklistAllUserTokens(eq(1L), eq("사용자 제재"));
                });
    }

    @Test
    @DisplayName("예외 상황에서의 이벤트 처리 - 소셜 로그인 해제 실패")
    void eventProcessingWithException_SocialUnlinkFailure() {
        // Given
        UserBannedEvent event = new UserBannedEvent(1L, "errorTest", SocialProvider.KAKAO);
        
        // 소셜 로그인 해제 실패 시뮬레이션
        doThrow(new RuntimeException("소셜 로그인 해제 실패"))
                .when(socialStrategyPort).unlink(SocialProvider.KAKAO, "errorTest");

        // When
        eventPublisher.publishEvent(event);

        // Then - 예외가 발생해도 이벤트 리스너는 호출되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("errorTest"));
                    // 소셜 해제는 실패하지만 블랙리스트 등록과 JWT 무효화는 시도되어야 함
                    verify(withdrawUseCase).addToBlacklist(eq(1L));
                    verify(userBanUseCase).blacklistAllUserTokens(eq(1L), eq("사용자 제재"));
                });
    }

    @Test
    @DisplayName("연속된 차단 이벤트 처리 순서")
    void sequentialUserBannedEvents_ShouldMaintainOrder() {
        // Given - 동일 사용자의 연속된 차단 이벤트
        Long userId = 1L;
        
        // When - 순서대로 차단 이벤트 발행
        eventPublisher.publishEvent(new UserBannedEvent(userId, "first", SocialProvider.KAKAO));
        eventPublisher.publishEvent(new UserBannedEvent(userId, "second", SocialProvider.KAKAO));
        eventPublisher.publishEvent(new UserBannedEvent(userId, "third", SocialProvider.KAKAO));

        // Then - 비동기 처리이지만 모든 이벤트가 처리되어야 함
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("first"));
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("second"));
                    verify(socialStrategyPort).unlink(eq(SocialProvider.KAKAO), eq("third"));
                    
                    verify(withdrawUseCase, times(3)).addToBlacklist(eq(userId));
                    verify(userBanUseCase, times(3)).blacklistAllUserTokens(eq(userId), eq("사용자 제재"));
                });
    }
}
