package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.auth.application.service.SocialWithdrawService;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private GlobalSocialStrategyPort strategyRegistryPort;

    @Mock
    private SocialStrategyPort socialStrategyPort;

    @InjectMocks
    private SocialWithdrawService socialWithdrawService;

    @Test
    @DisplayName("전략 조회와 연동 해제 호출")
    void shouldDelegateToStrategyWhenUnlinking() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "12345";
        given(strategyRegistryPort.getStrategy(provider)).willReturn(socialStrategyPort);

        // When
        socialWithdrawService.unlinkSocialAccount(provider, socialId);

        // Then
        verify(strategyRegistryPort).getStrategy(provider);
        verify(socialStrategyPort).unlink(provider, socialId);
    }
}
