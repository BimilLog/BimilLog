package jaeik.bimillog.unit.domain.auth;

import jaeik.bimillog.domain.auth.dto.LoginResultDTO;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.adapter.SocialStrategyAdapter;
import jaeik.bimillog.domain.auth.service.SocialLoginService;
import jaeik.bimillog.domain.auth.service.SocialLoginTransactionalService;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static jaeik.bimillog.testutil.fixtures.AuthTestFixtures.TEST_AUTH_CODE;
import static jaeik.bimillog.testutil.fixtures.AuthTestFixtures.TEST_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>SocialLoginService 단위 테스트</h2>
 * <p>외부 OAuth 호출 및 트랜잭션 위임 흐름 검증</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@DisplayName("SocialLoginService 단위 테스트")
@Tag("unit")
class SocialLoginServiceTest extends BaseUnitTest {

    @Mock private SocialStrategyAdapter strategyRegistryAdapter;
    @Mock private SocialStrategy kakaoStrategy;
    @Mock private SocialLoginTransactionalService socialLoginTransactionalService;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    @DisplayName("전략 실행 후 트랜잭션 서비스에 위임한다")
    void shouldDelegateToTransactionalServiceAfterFetchingProfile() {
        // Given
        SocialMemberProfile testMemberProfile = getTestMemberProfile();
        LoginResultDTO expectedResult = mock(LoginResultDTO.class);

        given(strategyRegistryAdapter.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
        given(kakaoStrategy.getSocialToken(TEST_AUTH_CODE, null)).willReturn(testMemberProfile);
        given(socialLoginTransactionalService.finishLogin(eq(TEST_PROVIDER), any(SocialMemberProfile.class)))
                .willReturn(expectedResult);

        // When
        LoginResultDTO result = socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, null);

        // Then
        assertThat(result).isSameAs(expectedResult);

        verify(socialLoginTransactionalService).finishLogin(eq(TEST_PROVIDER), any(SocialMemberProfile.class));
        verify(strategyRegistryAdapter).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).getSocialToken(TEST_AUTH_CODE, null);
    }

    @Test
    @DisplayName("트랜잭션 서비스 예외는 그대로 전파된다")
    void shouldPropagateExceptionFromTransactionalService() {
        // Given
        SocialMemberProfile testMemberProfile = getTestMemberProfile();
        CustomException expectedException = new CustomException(ErrorCode.AUTH_BLACKLIST_USER);

        given(strategyRegistryAdapter.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
        given(kakaoStrategy.getSocialToken(TEST_AUTH_CODE, null)).willReturn(testMemberProfile);
        given(socialLoginTransactionalService.finishLogin(TEST_PROVIDER, testMemberProfile))
                .willThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, null))
                .isSameAs(expectedException);

        verify(strategyRegistryAdapter).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).getSocialToken(TEST_AUTH_CODE, null);
        verify(socialLoginTransactionalService).finishLogin(TEST_PROVIDER, testMemberProfile);
    }

    @Test
    @DisplayName("소셜 전략 단계에서 실패하면 해당 예외를 전파한다")
    void shouldPropagateException_WhenAuthenticationFails() {
        // Given
        RuntimeException authException = new RuntimeException("Authentication failed");

        given(strategyRegistryAdapter.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
        given(kakaoStrategy.getSocialToken(TEST_AUTH_CODE, null)).willThrow(authException);

        // When & Then
        assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, null))
                .isSameAs(authException);

        verify(strategyRegistryAdapter).getStrategy(TEST_PROVIDER);
        verify(kakaoStrategy).getSocialToken(TEST_AUTH_CODE, null);
        verify(socialLoginTransactionalService, never()).finishLogin(any(), any());
    }

    private SocialMemberProfile getTestMemberProfile() {
        return SocialMemberProfile.of(
                AuthTestFixtures.TEST_SOCIAL_ID,
                AuthTestFixtures.TEST_EMAIL,
                AuthTestFixtures.TEST_PROVIDER,
                AuthTestFixtures.TEST_SOCIAL_NICKNAME,
                AuthTestFixtures.TEST_PROFILE_IMAGE,
                AuthTestFixtures.TEST_ACCESS_TOKEN,
                AuthTestFixtures.TEST_REFRESH_TOKEN
        );
    }
}
