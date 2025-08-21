package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.out.BlacklistPort;
import jaeik.growfarm.domain.auth.application.port.out.SaveUserPort;
import jaeik.growfarm.domain.auth.application.port.out.TempDataPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.auth.in.web.dto.LoginResponseDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResultDTO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>SocialLoginService 단위 테스트</h2>
 * <p>소셜 로그인 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SocialLoginService 단위 테스트")
class SocialLoginServiceTest {

    @Mock
    private SocialLoginPort socialLoginPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private TempDataPort tempDataPort;

    @Mock
    private BlacklistPort blacklistPort;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SocialLoginService socialLoginService;

    private SocialLoginUserData testUserData;
    private TokenDTO testTokenDTO;
    private LoginResultDTO existingUserResult;
    private LoginResultDTO newUserResult;

    @BeforeEach
    void setUp() {
        testUserData = new SocialLoginUserData("kakao123", "test@example.com", SocialProvider.KAKAO, "testUser", "profile.jpg", "fcm-token");
        testTokenDTO = TokenDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        existingUserResult = LoginResultDTO.builder()
                .loginType(LoginResultDTO.LoginType.EXISTING_USER)
                .userData(testUserData)
                .tokenDTO(testTokenDTO)
                .build();

        newUserResult = LoginResultDTO.builder()
                .loginType(LoginResultDTO.LoginType.NEW_USER)
                .userData(testUserData)
                .tokenDTO(testTokenDTO)
                .build();
    }

    @Test
    @DisplayName("기존 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenExistingUser() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String code = "auth-code";
        String fcmToken = "fcm-token";
        
        List<ResponseCookie> cookies = List.of(
                ResponseCookie.from("access_token", "access-token").build(),
                ResponseCookie.from("refresh_token", "refresh-token").build()
        );

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
            
            given(socialLoginPort.login(provider, code)).willReturn(existingUserResult);
            given(blacklistPort.existsByProviderAndSocialId(provider, testUserData.socialId())).willReturn(false);
            given(saveUserPort.handleExistingUserLogin(testUserData, testTokenDTO, fcmToken)).willReturn(cookies);

            // When
            LoginResponseDTO<?> result = socialLoginService.processSocialLogin(provider, code, fcmToken);

            // Then
            assertThat(result.getType()).isEqualTo(LoginResponseDTO.LoginType.EXISTING_USER);
            assertThat(result.getData()).isEqualTo(cookies);
            
            verify(socialLoginPort).login(provider, code);
            verify(blacklistPort).existsByProviderAndSocialId(provider, testUserData.socialId());
            verify(saveUserPort).handleExistingUserLogin(testUserData, testTokenDTO, fcmToken);
            verify(tempDataPort, never()).saveTempData(anyString(), any(), any());
        }
    }

    @Test
    @DisplayName("신규 사용자 소셜 로그인 성공")
    void shouldProcessSocialLogin_WhenNewUser() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String code = "auth-code";
        String fcmToken = "fcm-token";
        
        ResponseCookie tempCookie = ResponseCookie.from("temp_uuid", "uuid-123").build();

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
            
            given(socialLoginPort.login(provider, code)).willReturn(newUserResult);
            given(blacklistPort.existsByProviderAndSocialId(provider, testUserData.socialId())).willReturn(false);
            given(tempDataPort.saveTempDataAndCreateCookie(anyString(), eq(testUserData), eq(testTokenDTO))).willReturn(tempCookie);

            // When
            LoginResponseDTO<?> result = socialLoginService.processSocialLogin(provider, code, fcmToken);

            // Then
            assertThat(result.getType()).isEqualTo(LoginResponseDTO.LoginType.NEW_USER);
            assertThat(result.getData()).isEqualTo(tempCookie);
            
            verify(socialLoginPort).login(provider, code);
            verify(blacklistPort).existsByProviderAndSocialId(provider, testUserData.socialId());
            verify(tempDataPort).saveTempDataAndCreateCookie(anyString(), eq(testUserData), eq(testTokenDTO));
            verify(saveUserPort, never()).handleExistingUserLogin(any(), any(), anyString());
        }
    }

    @Test
    @DisplayName("블랙리스트 사용자 로그인 시 BLACKLIST_USER 예외 발생")
    void shouldThrowException_WhenBlacklistUser() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String code = "auth-code";
        String fcmToken = "fcm-token";

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
            
            given(socialLoginPort.login(provider, code)).willReturn(existingUserResult);
            given(blacklistPort.existsByProviderAndSocialId(provider, testUserData.socialId())).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(provider, code, fcmToken))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_USER);

            verify(socialLoginPort).login(provider, code);
            verify(blacklistPort).existsByProviderAndSocialId(provider, testUserData.socialId());
            verify(saveUserPort, never()).handleExistingUserLogin(any(), any(), anyString());
            verify(tempDataPort, never()).saveTempData(anyString(), any(), any());
        }
    }

    @Test
    @DisplayName("이미 로그인된 사용자가 로그인 시도 시 ALREADY_LOGIN 예외 발생")
    void shouldThrowException_WhenAlreadyLoggedIn() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String code = "auth-code";
        String fcmToken = "fcm-token";

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            UsernamePasswordAuthenticationToken authenticatedToken = 
                    new UsernamePasswordAuthenticationToken("user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
            
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authenticatedToken);

            // When & Then
            assertThatThrownBy(() -> socialLoginService.processSocialLogin(provider, code, fcmToken))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_LOGIN);

            verify(socialLoginPort, never()).login(any(), anyString());
            verify(blacklistPort, never()).existsByProviderAndSocialId(any(), anyString());
        }
    }

    @Test
    @DisplayName("인증 정보가 null인 경우 로그인 진행")
    void shouldProceedLogin_WhenAuthenticationIsNull() {
        // Given
        SocialProvider provider = SocialProvider.KAKAO;
        String code = "auth-code";
        String fcmToken = "fcm-token";
        
        List<ResponseCookie> cookies = List.of(
                ResponseCookie.from("access_token", "access-token").build()
        );

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(null);
            
            given(socialLoginPort.login(provider, code)).willReturn(existingUserResult);
            given(blacklistPort.existsByProviderAndSocialId(provider, testUserData.socialId())).willReturn(false);
            given(saveUserPort.handleExistingUserLogin(testUserData, testTokenDTO, fcmToken)).willReturn(cookies);

            // When
            LoginResponseDTO<?> result = socialLoginService.processSocialLogin(provider, code, fcmToken);

            // Then
            assertThat(result.getType()).isEqualTo(LoginResponseDTO.LoginType.EXISTING_USER);
            verify(socialLoginPort).login(provider, code);
        }
    }

    @Test
    @DisplayName("다양한 소셜 제공자로 로그인 테스트")
    void shouldProcessLogin_WithDifferentProviders() {
        // Given
        SocialProvider[] providers = {SocialProvider.KAKAO};
        String code = "auth-code";
        String fcmToken = "fcm-token";

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            for (SocialProvider provider : providers) {
                given(socialLoginPort.login(provider, code)).willReturn(existingUserResult);
                given(blacklistPort.existsByProviderAndSocialId(provider, testUserData.socialId())).willReturn(false);
                given(saveUserPort.handleExistingUserLogin(testUserData, testTokenDTO, fcmToken))
                        .willReturn(List.of(ResponseCookie.from("token", "value").build()));

                // When
                LoginResponseDTO<?> result = socialLoginService.processSocialLogin(provider, code, fcmToken);

                // Then
                assertThat(result.getType()).isEqualTo(LoginResponseDTO.LoginType.EXISTING_USER);
                verify(socialLoginPort).login(provider, code);
            }
        }
    }
}