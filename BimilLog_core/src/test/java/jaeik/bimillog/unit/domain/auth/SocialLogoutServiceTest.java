package jaeik.bimillog.unit.domain.auth;

import jaeik.bimillog.domain.auth.adapter.AuthToMemberAdapter;
import jaeik.bimillog.domain.auth.adapter.SocialStrategyAdapter;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
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

import static jaeik.bimillog.testutil.AuthTestFixtures.TEST_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * SocialLogoutService 단위 테스트
 * <p>회원 ID와 소셜 제공자를 통해 소셜 플랫폼 로그아웃을 처리하는 흐름을 검증합니다.</p>
 * <p>회원 미존재 시 예외 발생, 소셜 전략 위임, 로그아웃 실패 시 예외 전파 등의 시나리오를 검증합니다.</p>
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
    private AuthToMemberAdapter authToMemberAdapter;

    @InjectMocks
    private SocialLogoutService socialLogoutService;

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldSocialLogout_WhenValidMemberDetails() throws Exception {
        // given
        Long memberId = 100L;
        Member member = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);

        given(authToMemberAdapter.findById(memberId)).willReturn(member);
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);

        // when
        socialLogoutService.socialLogout(memberId, TEST_PROVIDER);

        // then
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).logout(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("회원이 존재하지 않는 경우 예외 발생")
    void shouldThrowException_WhenTokenNotFound() {
        // given
        given(authToMemberAdapter.findById(100L)).willThrow(new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // expect
        assertThatThrownBy(() -> socialLogoutService.socialLogout(100L, TEST_PROVIDER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_USER_NOT_FOUND);

        verify(strategyRegistry, never()).getStrategy(any());
    }

    @Test
    @DisplayName("소셜 로그아웃 실패 시 예외를 전파한다")
    void shouldContinueWhenSocialLogoutFails() throws Exception {
        // given
        Long memberId = 100L;
        Member member = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);

        given(authToMemberAdapter.findById(memberId)).willReturn(member);
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
        doThrow(new RuntimeException("logout failed")).when(kakaoStrategy).logout(anyString());

        // when
        assertThatThrownBy(() -> socialLogoutService.socialLogout(memberId, TEST_PROVIDER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("logout failed");

        verify(strategyRegistry).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).logout(anyString());
    }

}
