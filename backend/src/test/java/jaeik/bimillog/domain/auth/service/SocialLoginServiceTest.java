package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.AuthToUserPort;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.NewUserDetail;
import jaeik.bimillog.testutil.BaseAuthUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static jaeik.bimillog.testutil.AuthTestFixtures.TEST_PROVIDER;
import static jaeik.bimillog.testutil.AuthTestFixtures.createTempCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>SocialLoginService 단위 테스트</h2>
 * <p>소셜 로그인 서비스의 핵심 비즈니스 로직 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("SocialLoginService 단위 테스트")
@Tag("test")
class SocialLoginServiceTest extends BaseAuthUnitTest {

    @Mock private SocialStrategyRegistryPort strategyRegistry;
    @Mock private SocialStrategyPort kakaoStrategy;
    @Mock private AuthToUserPort authToUserPort;
    @Mock private BlacklistPort blacklistPort;
    @Mock private GlobalCookiePort globalCookiePort;
    @Mock private GlobalJwtPort globalJwtPort;

    private SocialLoginService socialLoginService;

    @BeforeEach
    void setUp() {
        socialLoginService = new SocialLoginService(
            strategyRegistry,
            authToUserPort,
            blacklistPort,
            globalCookiePort,
            globalJwtPort
        );
    }

    @Test
    @DisplayName("기존 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenExistingUser() {
        // Given
        String generatedAccessToken = "generated-access-token";
        String generatedRefreshToken = "generated-refresh-token";
        SocialUserProfile testUserProfile = getTestUserProfile();
        ExistingUserDetail existingUserDetail = getExistingUserDetail();
        List<ResponseCookie> jwtCookies = getJwtCookies();

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
            given(kakaoStrategy.authenticate(TEST_PROVIDER, TEST_AUTH_CODE)).willReturn(testUserProfile);
            given(blacklistPort.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
            given(authToUserPort.delegateUserData(TEST_PROVIDER, testUserProfile, TEST_FCM_TOKEN))
                .willReturn(existingUserDetail);
            given(globalJwtPort.generateAccessToken(existingUserDetail)).willReturn(generatedAccessToken);
            given(globalJwtPort.generateRefreshToken(existingUserDetail)).willReturn(generatedRefreshToken);
            given(globalCookiePort.generateJwtCookie(generatedAccessToken, generatedRefreshToken))
                .willReturn(jwtCookies);

            // When
            LoginResult result = socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN);

            // Then
            assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
            LoginResult.ExistingUser existingUserResponse = (LoginResult.ExistingUser) result;
            assertThat(existingUserResponse.cookies()).isEqualTo(jwtCookies);

            verify(strategyRegistry).getStrategy(TEST_PROVIDER);
            verify(kakaoStrategy).authenticate(TEST_PROVIDER, TEST_AUTH_CODE);
            verify(authToUserPort).delegateUserData(TEST_PROVIDER, testUserProfile, TEST_FCM_TOKEN);
            verify(globalJwtPort).generateAccessToken(existingUserDetail);
            verify(globalJwtPort).generateRefreshToken(existingUserDetail);
            verify(globalCookiePort).generateJwtCookie(generatedAccessToken, generatedRefreshToken);
        }
    }

    @Test
    @DisplayName("신규 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenNewUser() {
        // Given
        SocialUserProfile testUserProfile = getTestUserProfile();
        NewUserDetail newUserDetail = getNewUserDetail();
        ResponseCookie tempCookie = createTempCookie(newUserDetail.getUuid() != null ? newUserDetail.getUuid() : "test-uuid");

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
            given(kakaoStrategy.authenticate(TEST_PROVIDER, TEST_AUTH_CODE)).willReturn(testUserProfile);
            given(blacklistPort.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(false);
            given(authToUserPort.delegateUserData(TEST_PROVIDER, testUserProfile, TEST_FCM_TOKEN))
                .willReturn(newUserDetail);
            given(globalCookiePort.createTempCookie(newUserDetail)).willReturn(tempCookie);

            // When
            LoginResult result = socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN);

            // Then
            assertThat(result).isInstanceOf(LoginResult.NewUser.class);
            LoginResult.NewUser newUserResponse = (LoginResult.NewUser) result;
            assertThat(newUserResponse.uuid()).isEqualTo(newUserDetail.getUuid());
            assertThat(newUserResponse.tempCookie()).isNotNull();

            verify(strategyRegistry).getStrategy(TEST_PROVIDER);
            verify(kakaoStrategy).authenticate(TEST_PROVIDER, TEST_AUTH_CODE);
            verify(authToUserPort).delegateUserData(TEST_PROVIDER, testUserProfile, TEST_FCM_TOKEN);
            verify(globalCookiePort).createTempCookie(newUserDetail);
        }
    }

    @Test
    @DisplayName("블랙리스트 사용자 로그인 시 예외 발생")
    void shouldThrowException_WhenBlacklistedUser() {
        // Given
        SocialUserProfile testUserProfile = getTestUserProfile();

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
            given(kakaoStrategy.authenticate(TEST_PROVIDER, TEST_AUTH_CODE)).willReturn(testUserProfile);
            given(blacklistPort.existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isInstanceOf(AuthCustomException.class)
                    .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.BLACKLIST_USER);

            verify(strategyRegistry).getStrategy(TEST_PROVIDER);
            verify(kakaoStrategy).authenticate(TEST_PROVIDER, TEST_AUTH_CODE);
            verify(blacklistPort).existsByProviderAndSocialId(TEST_PROVIDER, TEST_SOCIAL_ID);
        }
    }

    @Test
    @DisplayName("이미 로그인된 사용자 소셜 로그인 시 예외 발생")
    void shouldThrowException_WhenAlreadyLoggedIn() {
        // Given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAuthenticatedUser(mockedSecurityContext);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isInstanceOf(AuthCustomException.class)
                    .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.ALREADY_LOGIN);
        }
    }

    @Test
    @DisplayName("소셜 인증 실패 시 예외 전파")
    void shouldPropagateException_WhenAuthenticationFails() {
        // Given
        RuntimeException authException = new RuntimeException("Authentication failed");

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
            given(kakaoStrategy.authenticate(TEST_PROVIDER, TEST_AUTH_CODE)).willThrow(authException);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(TEST_PROVIDER, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Authentication failed");

            verify(strategyRegistry).getStrategy(TEST_PROVIDER);
            verify(kakaoStrategy).authenticate(TEST_PROVIDER, TEST_AUTH_CODE);
            verify(blacklistPort, never()).existsByProviderAndSocialId(any(), any());
        }
    }


}