package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.adapter.AuthToMemberAdapter;
import jaeik.bimillog.domain.auth.adapter.SocialStrategyAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>SocialWithdrawService 테스트</h2>
 * <p>소셜 연동 해제 흐름이 전략 포트를 통해 실행되는지 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SocialWithdrawService 테스트")
@Tag("unit")
class SocialWithdrawServiceTest {

    @Mock
    private SocialStrategyAdapter strategyRegistryAdapter;

    @Mock
    private AuthToMemberAdapter authToMemberAdapter;

    @Mock
    private SocialStrategy socialStrategy;

    @InjectMocks
    private SocialWithdrawService socialWithdrawService;

    @Test
    @DisplayName("전략 조회와 연동 해제 호출")
    void shouldDelegateToStrategyWhenUnlinking() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "12345";
        Long memberId = 1L;
        Member testMember = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);
        String accessToken = testMember.getSocialToken().getAccessToken();

        given(authToMemberAdapter.findById(memberId)).willReturn(Optional.of(testMember));
        given(strategyRegistryAdapter.getStrategy(provider)).willReturn(socialStrategy);

        // When
        socialWithdrawService.unlinkSocialAccount(provider, socialId, memberId);

        // Then
        verify(authToMemberAdapter).findById(memberId);
        verify(strategyRegistryAdapter).getStrategy(provider);
        verify(socialStrategy).unlink(socialId, accessToken);
    }
}
