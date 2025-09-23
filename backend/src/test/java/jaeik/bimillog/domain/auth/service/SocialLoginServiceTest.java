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
import jaeik.bimillog.domain.user.entity.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

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
@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    private static final String TEST_SOCIAL_ID = "kakao123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PROFILE_IMAGE = "profile.jpg";
    private static final String TEST_ACCESS_TOKEN = "access-token";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final String TEST_AUTH_CODE = "auth-code";
    private static final String TEST_FCM_TOKEN = "fcm-token-123";
    
    @Mock private SocialStrategyRegistryPort strategyRegistry;
    @Mock private SocialStrategyPort kakaoStrategy;
    @Mock private AuthToUserPort authToUserPort;
    @Mock private BlacklistPort blacklistPort;
    private SocialLoginService socialLoginService;

    private SocialUserProfile testUserProfile;
    private Token testToken;

    @BeforeEach
    void setUp() {
        testToken = Token.createTemporaryToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN);
        testUserProfile = new SocialUserProfile(TEST_SOCIAL_ID, TEST_EMAIL, SocialProvider.KAKAO, TEST_USERNAME, TEST_PROFILE_IMAGE, testToken);

        socialLoginService = new SocialLoginService(
            strategyRegistry,
            authToUserPort,
            blacklistPort
        );
    }

    private void mockAnonymousAuthentication(MockedStatic<SecurityContextHolder> mockedSecurityContext) {
        SecurityContext securityContext = mock(SecurityContext.class);
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(securityContext.getAuthentication())
                .willReturn(new AnonymousAuthenticationToken("key", "anonymous", 
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }

    private void mockAuthenticatedUser(MockedStatic<SecurityContextHolder> mockedSecurityContext) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", 
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
    }

    @Test
    @DisplayName("기존 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenExistingUser() {
        // Given
        List<ResponseCookie> cookies = List.of(ResponseCookie.from("auth", "token").build());
        LoginResult.ExistingUser existingUserResult = new LoginResult.ExistingUser(cookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistry.getStrategy(SocialProvider.KAKAO)).willReturn(kakaoStrategy);
            given(kakaoStrategy.authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE)).willReturn(testUserProfile);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(false);
            given(authToUserPort.delegateUserData(SocialProvider.KAKAO, testUserProfile, TEST_FCM_TOKEN)).willReturn(existingUserResult);

            // When
            LoginResult result = socialLoginService.processSocialLogin(SocialProvider.KAKAO, TEST_AUTH_CODE, TEST_FCM_TOKEN);

            // Then
            assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
            LoginResult.ExistingUser existingUserResponse = (LoginResult.ExistingUser) result;
            assertThat(existingUserResponse.cookies()).isEqualTo(cookies);

            verify(strategyRegistry).getStrategy(SocialProvider.KAKAO);
            verify(kakaoStrategy).authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE);
            verify(authToUserPort).delegateUserData(SocialProvider.KAKAO, testUserProfile, TEST_FCM_TOKEN);
        }
    }

    @Test
    @DisplayName("신규 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenNewUser() {
        // Given
        ResponseCookie tempCookie = ResponseCookie.from("temp", "uuid").build();
        LoginResult.NewUser newUserResult = new LoginResult.NewUser("test-uuid", tempCookie);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistry.getStrategy(SocialProvider.KAKAO)).willReturn(kakaoStrategy);
            given(kakaoStrategy.authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE)).willReturn(testUserProfile);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(false);
            given(authToUserPort.delegateUserData(SocialProvider.KAKAO, testUserProfile, TEST_FCM_TOKEN)).willReturn(newUserResult);

            // When
            LoginResult result = socialLoginService.processSocialLogin(SocialProvider.KAKAO, TEST_AUTH_CODE, TEST_FCM_TOKEN);

            // Then
            assertThat(result).isInstanceOf(LoginResult.NewUser.class);
            LoginResult.NewUser newUserResponse = (LoginResult.NewUser) result;
            assertThat(newUserResponse.uuid()).isEqualTo("test-uuid");
            assertThat(newUserResponse.tempCookie()).isEqualTo(tempCookie);

            verify(strategyRegistry).getStrategy(SocialProvider.KAKAO);
            verify(kakaoStrategy).authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE);
            verify(authToUserPort).delegateUserData(SocialProvider.KAKAO, testUserProfile, TEST_FCM_TOKEN);
        }
    }

    @Test
    @DisplayName("블랙리스트 사용자 로그인 시 예외 발생")
    void shouldThrowException_WhenBlacklistedUser() {
        // Given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAnonymousAuthentication(mockedSecurityContext);

            given(strategyRegistry.getStrategy(SocialProvider.KAKAO)).willReturn(kakaoStrategy);
            given(kakaoStrategy.authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE)).willReturn(testUserProfile);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(SocialProvider.KAKAO, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isInstanceOf(AuthCustomException.class)
                    .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.BLACKLIST_USER);

            verify(strategyRegistry).getStrategy(SocialProvider.KAKAO);
            verify(kakaoStrategy).authenticate(SocialProvider.KAKAO, TEST_AUTH_CODE);
            verify(blacklistPort).existsByProviderAndSocialId(SocialProvider.KAKAO, TEST_SOCIAL_ID);
        }
    }

    @Test
    @DisplayName("이미 로그인된 사용자 소셜 로그인 시 예외 발생")
    void shouldThrowException_WhenAlreadyLoggedIn() {
        // Given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockAuthenticatedUser(mockedSecurityContext);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(SocialProvider.KAKAO, TEST_AUTH_CODE, TEST_FCM_TOKEN))
                    .isInstanceOf(AuthCustomException.class)
                    .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.ALREADY_LOGIN);
        }
    }
}