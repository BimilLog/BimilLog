package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialWithdrawService;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>SocialWithdrawService 단위 테스트</h2>
 * <p>소셜 탈퇴 서비스의 핵심 비즈니스 로직 테스트</p>
 * <p>소셜 플랫폼 연동 해제 기능을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SocialWithdrawService 단위 테스트")
class SocialWithdrawServiceTest {

    private static final String TEST_SOCIAL_ID = "kakao123456";
    private static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;

    @Mock
    private SocialStrategyRegistryPort strategyRegistryPort;

    @Mock
    private SocialStrategyPort socialStrategyPort;

    @InjectMocks
    private SocialWithdrawService socialWithdrawService;

    @BeforeEach
    void setUp() {
        // 기본 mock 설정
        given(strategyRegistryPort.getStrategy(TEST_PROVIDER)).willReturn(socialStrategyPort);
    }

    @Test
    @DisplayName("소셜 계정 연동 해제 성공")
    void shouldUnlinkSocialAccount_WhenValidRequest() {
        // Given
        doNothing().when(socialStrategyPort).unlink(TEST_PROVIDER, TEST_SOCIAL_ID);

        // When
        socialWithdrawService.unlinkSocialAccount(TEST_PROVIDER, TEST_SOCIAL_ID);

        // Then
        verify(strategyRegistryPort, times(1)).getStrategy(TEST_PROVIDER);
        verify(socialStrategyPort, times(1)).unlink(TEST_PROVIDER, TEST_SOCIAL_ID);
    }

    @Test
    @DisplayName("소셜 계정 연동 해제 시 전략 포트 호출 검증")
    void shouldGetCorrectStrategy_WhenUnlinking() {
        // Given
        doNothing().when(socialStrategyPort).unlink(any(), any());

        // When
        socialWithdrawService.unlinkSocialAccount(TEST_PROVIDER, TEST_SOCIAL_ID);

        // Then
        verify(strategyRegistryPort).getStrategy(TEST_PROVIDER);
        verify(socialStrategyPort).unlink(TEST_PROVIDER, TEST_SOCIAL_ID);
        verifyNoMoreInteractions(strategyRegistryPort, socialStrategyPort);
    }

    @Test
    @DisplayName("소셜 계정 연동 해제 실패 시 예외 전파")
    void shouldPropagateException_WhenUnlinkFails() {
        // Given
        RuntimeException expectedException = new RuntimeException("Unlink failed");
        doThrow(expectedException).when(socialStrategyPort).unlink(TEST_PROVIDER, TEST_SOCIAL_ID);

        // When & Then
        try {
            socialWithdrawService.unlinkSocialAccount(TEST_PROVIDER, TEST_SOCIAL_ID);
        } catch (RuntimeException e) {
            // 예외가 전파되는지 확인
            assert e.getMessage().equals("Unlink failed");
        }

        verify(strategyRegistryPort, times(1)).getStrategy(TEST_PROVIDER);
        verify(socialStrategyPort, times(1)).unlink(TEST_PROVIDER, TEST_SOCIAL_ID);
    }

    @Test
    @DisplayName("다른 소셜 플랫폼 제공자에 대한 연동 해제 테스트")
    void shouldHandleDifferentProviders_WhenUnlinking() {
        // Given
        SocialProvider[] providers = {SocialProvider.KAKAO};

        for (SocialProvider provider : providers) {
            // Reset mocks for each iteration
            reset(strategyRegistryPort, socialStrategyPort);
            given(strategyRegistryPort.getStrategy(provider)).willReturn(socialStrategyPort);
            doNothing().when(socialStrategyPort).unlink(provider, TEST_SOCIAL_ID);

            // When
            socialWithdrawService.unlinkSocialAccount(provider, TEST_SOCIAL_ID);

            // Then
            verify(strategyRegistryPort).getStrategy(provider);
            verify(socialStrategyPort).unlink(provider, TEST_SOCIAL_ID);
        }
    }
}