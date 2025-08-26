package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.out.BlacklistPort;
import jaeik.growfarm.domain.auth.application.port.out.SaveUserPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.application.port.out.TempDataPort;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.TokenVO;
import jaeik.growfarm.domain.auth.entity.LoginResult;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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

/**
 * <h2>SocialLoginService 단위 테스트</h2>
 * <p>소셜 로그인 서비스의 핵심 비즈니스 로직 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock private SocialLoginPort socialLoginPort;
    @Mock private SaveUserPort saveUserPort;
    @Mock private TempDataPort tempDataPort;
    @Mock private BlacklistPort blacklistPort;

    @InjectMocks
    private SocialLoginService socialLoginService;

    private SocialLoginUserData testUserData;
    private SocialLoginPort.SocialUserProfile testUserProfile;
    private TokenVO testTokenVO;
    private SocialLoginPort.LoginResult existingUserResult;
    private SocialLoginPort.LoginResult newUserResult;

    @BeforeEach
    void setUp() {
        testUserData = new SocialLoginUserData("kakao123", "test@example.com", SocialProvider.KAKAO, "testUser", "profile.jpg", "fcm-token");
        testUserProfile = new SocialLoginPort.SocialUserProfile("kakao123", "test@example.com", SocialProvider.KAKAO, "testUser", "profile.jpg");
        testTokenVO = TokenVO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        existingUserResult = new SocialLoginPort.LoginResult(testUserProfile, testTokenVO, false); // 기존 사용자
        newUserResult = new SocialLoginPort.LoginResult(testUserProfile, testTokenVO, true); // 신규 사용자
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

            given(socialLoginPort.login(SocialProvider.KAKAO, "auth-code")).willReturn(existingUserResult);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, "kakao123")).willReturn(false);
            given(saveUserPort.handleExistingUserLogin(testUserProfile, testTokenVO, fcmToken)).willReturn(cookies);

            // When
            LoginResult result = socialLoginService.processSocialLogin(SocialProvider.KAKAO, "auth-code", fcmToken);

            // Then
            assertThat(result).isInstanceOf(LoginResult.ExistingUser.class);
            LoginResult.ExistingUser existingUserResponse = (LoginResult.ExistingUser) result;
            assertThat(existingUserResponse.cookies()).isEqualTo(cookies);

            verify(socialLoginPort).login(SocialProvider.KAKAO, "auth-code");
            verify(saveUserPort).handleExistingUserLogin(testUserProfile, testTokenVO, fcmToken);
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

            given(socialLoginPort.login(SocialProvider.KAKAO, "auth-code")).willReturn(newUserResult);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, "kakao123")).willReturn(false);
            given(tempDataPort.createTempCookie(anyString())).willReturn(tempCookie);

            // When
            LoginResult result = socialLoginService.processSocialLogin(SocialProvider.KAKAO, "auth-code", "fcm-token");

            // Then
            assertThat(result).isInstanceOf(LoginResult.NewUser.class);
            LoginResult.NewUser newUserResponse = (LoginResult.NewUser) result;
            assertThat(newUserResponse.tempCookie()).isEqualTo(tempCookie);

            verify(socialLoginPort).login(SocialProvider.KAKAO, "auth-code");
            verify(tempDataPort).saveTempData(anyString(), eq(testUserProfile), eq(testTokenVO));
            verify(tempDataPort).createTempCookie(anyString());
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

            given(socialLoginPort.login(SocialProvider.KAKAO, "auth-code")).willReturn(existingUserResult);
            given(blacklistPort.existsByProviderAndSocialId(SocialProvider.KAKAO, "kakao123")).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(SocialProvider.KAKAO, "auth-code", "fcm-token"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_USER);

            verify(socialLoginPort).login(SocialProvider.KAKAO, "auth-code");
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
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(SocialProvider.KAKAO, "auth-code", "fcm-token"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_LOGIN);
        }
    }
}