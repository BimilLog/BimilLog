package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.AuthToUserPort;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.user.entity.userdetail.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.userdetail.NewUserDetail;
import jaeik.bimillog.testutil.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static jaeik.bimillog.testutil.AuthTestFixtures.*;
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
class SocialLoginServiceTest extends BaseUnitTest {

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
        String uuid = newUserDetail.getUuid() != null ? newUserDetail.getUuid() : "test-uuid";
        ResponseCookie tempCookie = ResponseCookie.from("temp", uuid)
                .path("/")
                .maxAge(60 * 10) // 10분
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();

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

    /**
     * 기존 사용자 상세 정보 획득 - SocialLoginServiceTest 전용
     */
    private ExistingUserDetail getExistingUserDetail() {
        Long settingId = 1L;
        if (getTestUser().getSetting() != null && getTestUser().getSetting().getId() != null) {
            settingId = getTestUser().getSetting().getId();
        }
        return AuthTestFixtures.createExistingUserDetail(getTestUser());
    }

    /**
     * 신규 사용자 상세 정보 획득 - SocialLoginServiceTest 전용
     */
    private NewUserDetail getNewUserDetail() {
        return NewUserDetail.builder()
                .uuid("test-uuid-123")
                .build();
    }

    /**
     * JWT 쿠키 획득 - SocialLoginServiceTest 전용
     */
    private List<ResponseCookie> getJwtCookies() {
        String accessToken = "test-access-token";
        String refreshToken = "test-refresh-token";

        return Arrays.asList(
                ResponseCookie.from("accessToken", accessToken)
                        .maxAge(3600) // 1 hour
                        .path("/")
                        .secure(true)
                        .httpOnly(true)
                        .sameSite("Strict")
                        .build(),
                ResponseCookie.from("refreshToken", refreshToken)
                        .maxAge(86400) // 24 hours
                        .path("/")
                        .secure(true)
                        .httpOnly(true)
                        .sameSite("Strict")
                        .build()
        );
    }

    /**
     * 테스트용 토큰 획듍 - SocialLoginServiceTest 전용
     */
    private Token getTestToken() {
        return Token.createTemporaryToken(AuthTestFixtures.TEST_ACCESS_TOKEN, AuthTestFixtures.TEST_REFRESH_TOKEN);
    }

    /**
     * 소셜 로그인 사용자 프로필 획듍 - SocialLoginServiceTest 전용
     */
    private SocialUserProfile getTestUserProfile() {
        return new SocialUserProfile(
                AuthTestFixtures.TEST_SOCIAL_ID,
                AuthTestFixtures.TEST_EMAIL,
                AuthTestFixtures.TEST_PROVIDER,
                AuthTestFixtures.TEST_SOCIAL_NICKNAME,
                AuthTestFixtures.TEST_PROFILE_IMAGE,
                getTestToken()
        );
    }
}