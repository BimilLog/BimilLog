package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.out.LoadTokenPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageDeleteDataPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.event.UserLoggedOutEvent;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
/**
 * <h2>LogoutService 단위 테스트</h2>
 * <p>로그아웃 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutService 단위 테스트")
class LogoutServiceTest {

    @Mock
    private ManageDeleteDataPort manageDeleteDataPort;

    @Mock
    private SocialLoginPort socialLoginPort;

    @Mock
    private LoadTokenPort loadTokenPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private LogoutService logoutService;

    private Token testToken;
    private List<ResponseCookie> logoutCookies;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .id(100L)
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .build();

        testToken = Token.builder()
                .id(200L)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .users(testUser)
                .build();

        logoutCookies = List.of(
                ResponseCookie.from("access_token", "").maxAge(0).build(),
                ResponseCookie.from("refresh_token", "").maxAge(0).build()
        );
    }

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldLogout_WhenValidUserDetails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(testToken));
        given(manageDeleteDataPort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = logoutService.logout(userDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);
            assertThat(result).hasSize(2);

            // 이벤트 발행 검증
            ArgumentCaptor<UserLoggedOutEvent> eventCaptor = ArgumentCaptor.forClass(UserLoggedOutEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            
            UserLoggedOutEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.userId()).isEqualTo(100L);
            assertThat(capturedEvent.tokenId()).isEqualTo(200L);

            // SecurityContext 클리어 검증
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);

            // 소셜 로그아웃 검증
            verify(loadTokenPort).findById(200L);
            verify(socialLoginPort).logout(SocialProvider.KAKAO, "access-token");
            verify(manageDeleteDataPort).getLogoutCookies();
        }
    }

    @Test
    @DisplayName("토큰이 존재하지 않는 경우 로그아웃 처리")
    void shouldLogout_WhenTokenNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.empty());
        given(manageDeleteDataPort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = logoutService.logout(userDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);

            // 이벤트는 여전히 발행되어야 함
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            
            // 소셜 로그아웃은 호출되지 않아야 함
            verify(socialLoginPort, never()).logout(any(), any());
            
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("사용자가 null인 토큰으로 로그아웃 처리")
    void shouldLogout_WhenTokenHasNullUser() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        
        Token tokenWithNullUser = Token.builder()
                .id(200L)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .users(null)
                .build();

        given(loadTokenPort.findById(200L)).willReturn(Optional.of(tokenWithNullUser));
        given(manageDeleteDataPort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = logoutService.logout(userDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);

            // 이벤트는 발행되어야 함
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            
            // 소셜 로그아웃은 호출되지 않아야 함 (user가 null이므로)
            verify(socialLoginPort, never()).logout(any(), any());
            
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("소셜 로그아웃 실패 시 전체 로그아웃 실패")
    void shouldThrowException_WhenSocialLogoutFails() {
        // Given
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(testToken));
        doThrow(new RuntimeException("소셜 로그아웃 실패")).when(socialLoginPort)
                .logout(SocialProvider.KAKAO, "access-token");

        // When & Then
        assertThatThrownBy(() -> logoutService.logout(userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGOUT_FAIL);

        verify(loadTokenPort).findById(200L);
        verify(socialLoginPort).logout(SocialProvider.KAKAO, "access-token");
        
        // 실패 시 이벤트 발행 및 쿠키 생성은 호출되지 않아야 함
        verify(eventPublisher, never()).publishEvent(any());
        verify(manageDeleteDataPort, never()).getLogoutCookies();
    }

    @Test
    @DisplayName("이벤트 발행 실패 시 전체 로그아웃 실패")
    void shouldThrowException_WhenEventPublishingFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(testToken));
        doThrow(new RuntimeException("이벤트 발행 실패")).when(eventPublisher)
                .publishEvent(any(UserLoggedOutEvent.class));

        // When & Then
        assertThatThrownBy(() -> logoutService.logout(userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGOUT_FAIL);

        verify(socialLoginPort).logout(SocialProvider.KAKAO, "access-token");
        verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
        
        // 실패 시 쿠키 생성은 호출되지 않아야 함
        verify(manageDeleteDataPort, never()).getLogoutCookies();
    }

    @Test
    @DisplayName("로그아웃 쿠키 생성 실패 시 전체 로그아웃 실패")
    void shouldThrowException_WhenCookieGenerationFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(testToken));
        doThrow(new RuntimeException("쿠키 생성 실패")).when(manageDeleteDataPort).getLogoutCookies();

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When & Then
            assertThatThrownBy(() -> logoutService.logout(userDetails))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGOUT_FAIL);

            verify(socialLoginPort).logout(SocialProvider.KAKAO, "access-token");
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
            verify(manageDeleteDataPort).getLogoutCookies();
        }
    }

    @Test
    @DisplayName("소셜 로그아웃만 별도 테스트")
    void shouldLogoutSocial_WhenValidToken() {
        // Given
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(testToken));

        // When
        logoutService.logoutSocial(userDetails);

        // Then
        verify(loadTokenPort).findById(200L);
        verify(socialLoginPort).logout(SocialProvider.KAKAO, "access-token");
    }

    @Test
    @DisplayName("다양한 소셜 제공자에 대한 로그아웃 테스트")
    void shouldLogout_WithDifferentSocialProviders() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        
        SocialProvider[] providers = {SocialProvider.KAKAO};

        for (SocialProvider provider : providers) {
            User user = User.builder()
                    .id(100L)
                    .socialId("social123")
                    .provider(provider)
                    .userName("testUser")
                    .build();

            Token token = Token.builder()
                    .id(200L)
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .users(user)
                    .build();

            given(loadTokenPort.findById(200L)).willReturn(Optional.of(token));
            given(manageDeleteDataPort.getLogoutCookies()).willReturn(logoutCookies);

            try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
                // When
                List<ResponseCookie> result = logoutService.logout(userDetails);

                // Then
                assertThat(result).isEqualTo(logoutCookies);
                verify(socialLoginPort).logout(provider, "access-token");
            }
        }
    }
}