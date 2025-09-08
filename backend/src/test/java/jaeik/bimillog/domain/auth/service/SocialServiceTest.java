package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
//TODO 서비스 클래스 통합으로 테스트 코드 추가 필요성 검토 필요
/**
 * <h2>SocialService 단위 테스트</h2>
 * <p>소셜 로그인 서비스의 핵심 비즈니스 로직 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock private SocialPort socialPort;
    @Mock private SaveUserPort saveUserPort;
    @Mock private RedisUserDataPort redisUserDataPort;
    @Mock private BlacklistPort blacklistPort;

    @InjectMocks
    private SocialService socialService;

    private LoginResult.SocialUserProfile testUserProfile;
    private Token testToken;
    private LoginResult.SocialLoginData existingUserResult;
    private LoginResult.SocialLoginData newUserResult;

    @BeforeEach
    void setUp() {
        testUserProfile = new LoginResult.SocialUserProfile("kakao123", "test@example.com", SocialProvider.KAKAO, "testUser", "profile.jpg");
        testToken = Token.createTemporaryToken("access-token", "refresh-token");

        existingUserResult = new LoginResult.SocialLoginData(testUserProfile, testToken, false); // 기존 사용자
        newUserResult = new LoginResult.SocialLoginData(testUserProfile, testToken, true); // 신규 사용자
    }

    @Test
    @DisplayName("기존 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenExistingUser() {
        // Given
        String fcmToken = "fcm-token-123";
        List<ResponseCookie> cookies = List.of(ResponseCookie.from("auth", "token").build());
        
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            given(socialPort.login(SocialProvider.KAKAO, "auth-code")).willReturn(existingUserResult);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, "kakao123")).willReturn(false);
            given(saveUserPort.handleExistingUserLogin(testUserProfile, testToken, fcmToken)).willReturn(cookies);

            // When
            LoginResult result = socialService.processSocialLogin(SocialProvider.KAKAO, "auth-code", fcmToken);

            // Then
            assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
            LoginResult.ExistingUser existingUserResponse = (LoginResult.ExistingUser) result;
            assertThat(existingUserResponse.cookies()).isEqualTo(cookies);

            verify(socialPort).login(SocialProvider.KAKAO, "auth-code");
            verify(saveUserPort).handleExistingUserLogin(testUserProfile, testToken, fcmToken);
        }
    }

    @Test
    @DisplayName("신규 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenNewUser() {
        // Given
        ResponseCookie tempCookie = ResponseCookie.from("temp", "uuid").build();
        
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            given(socialPort.login(SocialProvider.KAKAO, "auth-code")).willReturn(newUserResult);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, "kakao123")).willReturn(false);
            given(redisUserDataPort.createTempCookie(anyString())).willReturn(tempCookie);

            // When
            LoginResult result = socialService.processSocialLogin(SocialProvider.KAKAO, "auth-code", "fcm-token");

            // Then
            assertThat(result).isInstanceOf(LoginResult.NewUser.class);
            LoginResult.NewUser newUserResponse = (LoginResult.NewUser) result;
            assertThat(newUserResponse.tempCookie()).isEqualTo(tempCookie);

            verify(socialPort).login(SocialProvider.KAKAO, "auth-code");
            verify(redisUserDataPort).saveTempData(anyString(), eq(testUserProfile), eq(testToken), eq("fcm-token"));
            verify(redisUserDataPort).createTempCookie(anyString());
        }
    }

    @Test
    @DisplayName("블랙리스트 사용자 로그인 시 예외 발생")
    void shouldThrowException_WhenBlacklistedUser() {
        // Given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            given(socialPort.login(SocialProvider.KAKAO, "auth-code")).willReturn(existingUserResult);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, "kakao123")).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> socialService.processSocialLogin(SocialProvider.KAKAO, "auth-code", "fcm-token"))
                    .isInstanceOf(AuthCustomException.class)
                    .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.BLACKLIST_USER);

            verify(socialPort).login(SocialProvider.KAKAO, "auth-code");
            verify(blacklistPort).existsByProviderAndSocialId(SocialProvider.KAKAO, "kakao123");
        }
    }

    @Test
    @DisplayName("이미 로그인된 사용자 소셜 로그인 시 예외 발생")
    void shouldThrowException_WhenAlreadyLoggedIn() {
        // Given
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
            
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);

            // When & Then
            assertThatThrownBy(() -> socialService.processSocialLogin(SocialProvider.KAKAO, "auth-code", "fcm-token"))
                    .isInstanceOf(AuthCustomException.class)
                    .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.ALREADY_LOGIN);
        }
    }
}