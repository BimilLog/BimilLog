package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialWithdrawService;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.testutil.BaseAuthUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("SocialWithdrawService 단위 테스트")
class SocialWithdrawServiceTest extends BaseAuthUnitTest {

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
        assertThatThrownBy(() -> socialWithdrawService.unlinkSocialAccount(TEST_PROVIDER, TEST_SOCIAL_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unlink failed");

        verify(strategyRegistryPort, times(1)).getStrategy(TEST_PROVIDER);
        verify(socialStrategyPort, times(1)).unlink(TEST_PROVIDER, TEST_SOCIAL_ID);
    }








}