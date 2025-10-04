package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.service.SocialLogoutService;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static jaeik.bimillog.testutil.AuthTestFixtures.TEST_ACCESS_TOKEN;
import static jaeik.bimillog.testutil.AuthTestFixtures.TEST_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("SocialLogoutService 단위 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
class SocialLogoutServiceTest extends BaseUnitTest {

    @Mock
    private GlobalSocialStrategyPort strategyRegistry;

    @Mock
    private SocialPlatformStrategy kakaoPlatformStrategy;

    @Mock
    private SocialAuthStrategy kakaoAuthStrategy;

    @Mock
    private GlobalKakaoTokenQueryPort globalKakaoTokenQueryPort;

    @InjectMocks
    private SocialLogoutService socialLogoutService;

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldSocialLogout_WhenValidMemberDetails() throws Exception {
        // given
        Long memberId = 100L;
        KakaoToken kakaoToken = KakaoToken.createKakaoToken(TEST_ACCESS_TOKEN, "kakao-refresh-token");

        given(globalKakaoTokenQueryPort.findByMemberId(memberId)).willReturn(Optional.of(kakaoToken));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
        given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);

        // when
        socialLogoutService.socialLogout(memberId, TEST_PROVIDER, 200L);

        // then
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoPlatformStrategy).auth();
        verify(kakaoAuthStrategy).logout(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isEqualTo(TEST_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("토큰이 존재하지 않는 경우 예외 발생")
    void shouldThrowException_WhenTokenNotFound() {
        // given
        given(globalKakaoTokenQueryPort.findByMemberId(100L)).willReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> socialLogoutService.socialLogout(100L, TEST_PROVIDER, 200L))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.NOT_FIND_TOKEN);

        verify(strategyRegistry, never()).getStrategy(any());
    }

    @Test
    @DisplayName("소셜 로그아웃 실패 시에도 나머지 플로우는 수행")
    void shouldContinueWhenSocialLogoutFails() throws Exception {
        // given
        Long memberId = 100L;
        KakaoToken kakaoToken = KakaoToken.createKakaoToken(TEST_ACCESS_TOKEN, "kakao-refresh-token");

        given(globalKakaoTokenQueryPort.findByMemberId(memberId)).willReturn(Optional.of(kakaoToken));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
        given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);
        doThrow(new RuntimeException("logout failed")).when(kakaoAuthStrategy).logout(TEST_ACCESS_TOKEN);

        // when
        assertThatThrownBy(() -> socialLogoutService.socialLogout(memberId, TEST_PROVIDER, 200L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("logout failed");

        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoPlatformStrategy).auth();
        verify(kakaoAuthStrategy).logout(TEST_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("다양한 사용자 정보로 로그아웃 처리")
    void shouldHandleDifferentMemberDetails() throws Exception {
        // given
        Long adminMemberId = 999L;
        KakaoToken kakaoToken = KakaoToken.createKakaoToken(TEST_ACCESS_TOKEN, "kakao-refresh-token");

        given(globalKakaoTokenQueryPort.findByMemberId(adminMemberId)).willReturn(Optional.of(kakaoToken));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
        given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);

        // when
        socialLogoutService.socialLogout(adminMemberId, TEST_PROVIDER, 888L);

        // then
        verify(kakaoAuthStrategy).logout(TEST_ACCESS_TOKEN);
    }
}
