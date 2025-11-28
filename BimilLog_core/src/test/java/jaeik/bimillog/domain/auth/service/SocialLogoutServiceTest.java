package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.global.out.GlobalSocialStrategyAdapter;
import jaeik.bimillog.domain.global.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static jaeik.bimillog.testutil.fixtures.AuthTestFixtures.TEST_ACCESS_TOKEN;
import static jaeik.bimillog.testutil.fixtures.AuthTestFixtures.TEST_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("SocialLogoutService 단위 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
class SocialLogoutServiceTest extends BaseUnitTest {

    @Mock
    private GlobalSocialStrategyAdapter strategyRegistry;

    @Mock
    private SocialPlatformStrategy kakaoPlatformStrategy;

    @Mock
    private SocialAuthStrategy kakaoAuthStrategy;

    @Mock
    private GlobalMemberQueryAdapter globalMemberQueryAdapter;

    @InjectMocks
    private SocialLogoutService socialLogoutService;

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldSocialLogout_WhenValidMemberDetails() throws Exception {
        // given
        Long memberId = 100L;
        SocialToken socialToken = SocialToken.createSocialToken(TEST_ACCESS_TOKEN, "kakao-refresh-token");
        Member member = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);

        given(globalMemberQueryAdapter.findById(memberId)).willReturn(Optional.of(member));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
        given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);

        // when
        socialLogoutService.socialLogout(memberId, TEST_PROVIDER);

        // then
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoPlatformStrategy).auth();
        verify(kakaoAuthStrategy).logout(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("토큰이 존재하지 않는 경우 예외 발생")
    void shouldThrowException_WhenTokenNotFound() {
        // given
        given(globalMemberQueryAdapter.findById(100L)).willReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> socialLogoutService.socialLogout(100L, TEST_PROVIDER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_NOT_FIND_TOKEN);

        verify(strategyRegistry, never()).getStrategy(any());
    }

    @Test
    @DisplayName("소셜 로그아웃 실패 시에도 나머지 플로우는 수행")
    void shouldContinueWhenSocialLogoutFails() throws Exception {
        // given
        Long memberId = 100L;
        Member member = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);

        given(globalMemberQueryAdapter.findById(memberId)).willReturn(Optional.of(member));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
        given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);
        doThrow(new RuntimeException("logout failed")).when(kakaoAuthStrategy).logout(anyString());

        // when
        assertThatThrownBy(() -> socialLogoutService.socialLogout(memberId, TEST_PROVIDER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("logout failed");

        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoPlatformStrategy).auth();
        verify(kakaoAuthStrategy).logout(anyString());
    }

    @Test
    @DisplayName("다양한 사용자 정보로 로그아웃 처리")
    void shouldHandleDifferentMemberDetails() throws Exception {
        // given
        Long adminMemberId = 999L;
        Member adminMember = TestMembers.copyWithId(TestMembers.MEMBER_1, adminMemberId);

        given(globalMemberQueryAdapter.findById(adminMemberId)).willReturn(Optional.of(adminMember));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
        given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);

        // when
        socialLogoutService.socialLogout(adminMemberId, TEST_PROVIDER);

        // then
        verify(kakaoAuthStrategy).logout(anyString());
    }
}
