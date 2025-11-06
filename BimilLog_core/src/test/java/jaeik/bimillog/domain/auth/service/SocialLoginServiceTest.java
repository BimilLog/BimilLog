package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.testutil.fixtures.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;

import static jaeik.bimillog.testutil.fixtures.AuthTestFixtures.*;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialLoginServiceTest extends BaseUnitTest {

    @Mock private GlobalSocialStrategyPort strategyRegistryPort;
    @Mock private SocialPlatformStrategy kakaoPlatformStrategy;
    @Mock private SocialAuthStrategy kakaoAuthStrategy;
    @Mock private SocialLoginTransactionalService socialLoginTransactionalService;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    @DisplayName("전략 실행 후 트랜잭션 서비스에 위임하고 FCM 토큰을 주입한다")
    void shouldDelegateToTransactionalServiceAfterFetchingProfile() {
        // Given
        SocialMemberProfile testMemberProfile = getTestMemberProfile();
        LoginResult expectedResult = mock(LoginResult.ExistingUser.class);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistryPort.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
            given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);
            given(kakaoAuthStrategy.getSocialToken(TEST_AUTH_CODE)).willReturn(testMemberProfile);
            given(socialLoginTransactionalService.finishLogin(eq(TEST_PROVIDER), any(SocialMemberProfile.class)))
                    .willReturn(expectedResult);

            // When
            LoginResult result = socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN);

            // Then
            assertThat(result).isSameAs(expectedResult);

            ArgumentCaptor<SocialMemberProfile> profileCaptor = ArgumentCaptor.forClass(SocialMemberProfile.class);
            verify(socialLoginTransactionalService).finishLogin(eq(TEST_PROVIDER), profileCaptor.capture());
            assertThat(profileCaptor.getValue().getFcmToken()).isEqualTo(TEST_FCM_TOKEN);

            verify(strategyRegistryPort).getStrategy(TEST_PROVIDER);
            verify(kakaoPlatformStrategy).auth();
            verify(kakaoAuthStrategy).getSocialToken(TEST_AUTH_CODE);
        }
    }

    @Test
    @DisplayName("트랜잭션 서비스 예외는 그대로 전파된다")
    void shouldPropagateExceptionFromTransactionalService() {
        // Given
        SocialMemberProfile testMemberProfile = getTestMemberProfile();
        AuthCustomException expectedException = new AuthCustomException(AuthErrorCode.BLACKLIST_USER);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistryPort.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
            given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);
            given(kakaoAuthStrategy.getSocialToken(TEST_AUTH_CODE)).willReturn(testMemberProfile);
            given(socialLoginTransactionalService.finishLogin(TEST_PROVIDER, testMemberProfile))
                    .willThrow(expectedException);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isSameAs(expectedException);

            verify(strategyRegistryPort).getStrategy(TEST_PROVIDER);
            verify(kakaoPlatformStrategy).auth();
            verify(kakaoAuthStrategy).getSocialToken(TEST_AUTH_CODE);
            verify(socialLoginTransactionalService).finishLogin(TEST_PROVIDER, testMemberProfile);
        }
    }

    @Test
    @DisplayName("이미 로그인된 사용자는 차단한다")
    void shouldThrowException_WhenAlreadyLoggedIn() {
        // Given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAuthenticatedMember(mockedSecurityContext);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isInstanceOf(AuthCustomException.class)
                    .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.ALREADY_LOGIN);

            verify(strategyRegistryPort, never()).getStrategy(any());
            verify(socialLoginTransactionalService, never()).finishLogin(any(), any());
        }
    }

    @Test
    @DisplayName("소셜 전략 단계에서 실패하면 해당 예외를 전파한다")
    void shouldPropagateException_WhenAuthenticationFails() {
        // Given
        RuntimeException authException = new RuntimeException("Authentication failed");

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistryPort.getStrategy(TEST_PROVIDER)).willReturn(kakaoPlatformStrategy);
            given(kakaoPlatformStrategy.auth()).willReturn(kakaoAuthStrategy);
            given(kakaoAuthStrategy.getSocialToken(TEST_AUTH_CODE)).willThrow(authException);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isSameAs(authException);

            verify(strategyRegistryPort).getStrategy(TEST_PROVIDER);
            verify(kakaoPlatformStrategy).auth();
            verify(kakaoAuthStrategy).getSocialToken(TEST_AUTH_CODE);
            verify(socialLoginTransactionalService, never()).finishLogin(any(), any());
        }
    }

    private SocialMemberProfile getTestMemberProfile() {
        return SocialMemberProfile.of(
                AuthTestFixtures.TEST_SOCIAL_ID,
                AuthTestFixtures.TEST_EMAIL,
                AuthTestFixtures.TEST_PROVIDER,
                AuthTestFixtures.TEST_SOCIAL_NICKNAME,
                AuthTestFixtures.TEST_PROFILE_IMAGE,
                AuthTestFixtures.TEST_ACCESS_TOKEN,
                AuthTestFixtures.TEST_REFRESH_TOKEN,
                null
        );
    }
}
