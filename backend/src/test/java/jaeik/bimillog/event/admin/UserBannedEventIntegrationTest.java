package jaeik.bimillog.event.admin;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.BlacklistUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import jaeik.bimillog.testutil.EventTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * <h2>사용자 차단 이벤트 워크플로우 통합 테스트</h2>
 * <p>관리자가 사용자를 차단할 때 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("사용자 차단 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class UserBannedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private WithdrawUseCase withdrawUseCase;

    @MockitoBean
    private BlacklistUseCase blacklistUseCase;

    @MockitoBean
    private SocialWithdrawUseCase socialWithdrawUseCase;

    @Test
    @DisplayName("사용자 차단 이벤트 워크플로우 - BAN 상태 변경과 블랙리스트 등록까지 완료")
    void userBannedEventWorkflow_ShouldCompleteBanAndBlacklist() {
        // Given
        Long userId = 1L;
        String socialId = "testKakaoId123";
        SocialProvider provider = SocialProvider.KAKAO;
        UserBannedEvent event = EventTestDataBuilder.createUserBannedEvent(userId, socialId, provider);

        // When & Then
        publishAndVerify(event, () -> {
            // 사용자 BAN 상태로 변경
            verify(withdrawUseCase).banUser(eq(userId));
            // 사용자 블랙리스트 등록
            verify(blacklistUseCase).addToBlacklist(eq(userId), eq(socialId), eq(provider));
            // JWT 토큰 무효화
            verify(blacklistUseCase).blacklistAllUserTokens(eq(userId));
            // 소셜 연결 해제
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(provider), eq(socialId));
        });
    }

    @Test
    @DisplayName("여러 사용자 차단 이벤트 동시 처리")
    void multipleUserBannedEvents_ShouldProcessConcurrently() {
        // Given
        UserBannedEvent event1 = EventTestDataBuilder.createUserBannedEvent(1L, "kakao123", SocialProvider.KAKAO);
        UserBannedEvent event2 = EventTestDataBuilder.createUserBannedEvent(2L, "kakao456", SocialProvider.KAKAO);
        UserBannedEvent event3 = EventTestDataBuilder.createUserBannedEvent(3L, "kakao789", SocialProvider.KAKAO);

        // When & Then - 동시에 여러 사용자 차단 이벤트 발행
        publishEventsAndVerify(new Object[]{event1, event2, event3}, () -> {
            verify(withdrawUseCase).banUser(eq(1L));
            verify(withdrawUseCase).banUser(eq(2L));
            verify(withdrawUseCase).banUser(eq(3L));

            verify(blacklistUseCase).addToBlacklist(eq(1L), eq("kakao123"), eq(SocialProvider.KAKAO));
            verify(blacklistUseCase).addToBlacklist(eq(2L), eq("kakao456"), eq(SocialProvider.KAKAO));
            verify(blacklistUseCase).addToBlacklist(eq(3L), eq("kakao789"), eq(SocialProvider.KAKAO));

            verify(blacklistUseCase).blacklistAllUserTokens(eq(1L));
            verify(blacklistUseCase).blacklistAllUserTokens(eq(2L));
            verify(blacklistUseCase).blacklistAllUserTokens(eq(3L));

            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("kakao123"));
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("kakao456"));
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("kakao789"));
        });
    }

    @Test
    @DisplayName("다양한 소셜 제공자의 차단 이벤트 처리")
    void userBannedEventsWithVariousProviders_ShouldProcessCorrectly() {
        // Given - 다양한 소셜 제공자
        UserBannedEvent kakaoEvent = EventTestDataBuilder.createUserBannedEvent(1L, "kakaoUser", SocialProvider.KAKAO);
        // 향후 다른 소셜 제공자 추가 시 테스트 확장 가능

        // When & Then - 모든 제공자별로 적절히 처리되어야 함
        publishAndVerify(kakaoEvent, () -> {
            verify(withdrawUseCase).banUser(eq(1L));
            verify(blacklistUseCase).addToBlacklist(eq(1L), eq("kakaoUser"), eq(SocialProvider.KAKAO));
            verify(blacklistUseCase).blacklistAllUserTokens(eq(1L));
            verify(socialWithdrawUseCase).unlinkSocialAccount(eq(SocialProvider.KAKAO), eq("kakaoUser"));
        });
    }
}
