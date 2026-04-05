package jaeik.bimillog.unit.domain.auth;

import jaeik.bimillog.domain.auth.adapter.SocialStrategyAdapter;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.auth.service.SocialTokenService;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static jaeik.bimillog.testutil.AuthTestFixtures.TEST_PROVIDER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * SocialLogoutService 단위 테스트
 */
@DisplayName("SocialLogoutService 단위 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
class SocialLogoutServiceTest extends BaseUnitTest {

    @Mock
    private SocialStrategyAdapter strategyRegistry;

    @Mock
    private SocialStrategy kakaoStrategy;

    @Mock
    private SocialTokenService socialTokenService;

    @InjectMocks
    private SocialLogoutService socialLogoutService;

    @Test
    @DisplayName("소셜 계정 연동 해제 - 토큰 있는 경우")
    void shouldUnlinkSocialAccount_WhenTokenExists() {
        // given
        Long memberId = 100L;
        String socialId = "social123";
        Member member = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);
        SocialToken socialToken = SocialToken.createSocialToken("test-access-token", "test-refresh-token", member);

        given(socialTokenService.getSocialToken(memberId)).willReturn(Optional.of(socialToken));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);

        // when
        socialLogoutService.unlinkSocialAccount(TEST_PROVIDER, socialId, memberId);

        // then
        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).unlink(socialId, "test-access-token");
    }

    @Test
    @DisplayName("소셜 계정 연동 해제 - 토큰 없는 경우 accessToken null로 호출")
    void shouldUnlinkSocialAccount_WhenTokenNotFound() {
        // given
        Long memberId = 100L;
        String socialId = "social123";

        given(socialTokenService.getSocialToken(memberId)).willReturn(Optional.empty());
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);

        // when
        socialLogoutService.unlinkSocialAccount(TEST_PROVIDER, socialId, memberId);

        // then
        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).unlink(socialId, null);
    }

    @Test
    @DisplayName("강제 로그아웃 - 소셜 전략에 위임")
    void shouldForceLogout() {
        // given
        String socialId = "social123";
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);

        // when
        socialLogoutService.forceLogout(socialId, TEST_PROVIDER);

        // then
        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).forceLogout(socialId);
    }
}
