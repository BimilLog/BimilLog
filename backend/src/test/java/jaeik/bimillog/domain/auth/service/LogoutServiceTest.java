package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.auth.application.port.out.LoadTokenPort;
import jaeik.bimillog.domain.auth.application.service.LogoutService;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
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
    private DeleteUserPort deleteUserPort;

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

    private List<ResponseCookie> logoutCookies;

    @BeforeEach
    void setUp() {
        logoutCookies = List.of(
                ResponseCookie.from("jwt_access_token", "")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(true)
                        .build(),
                ResponseCookie.from("jwt_refresh_token", "")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(true)
                        .build()
        );
    }

    private Token createMockTokenWithUser() {
        User mockUser = mock(User.class);
        Token mockToken = mock(Token.class);
        
        given(mockUser.getProvider()).willReturn(SocialProvider.KAKAO);
        given(mockToken.getUsers()).willReturn(mockUser);
        given(mockToken.getAccessToken()).willReturn("mock-access-token");
        
        return mockToken;
    }

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldLogout_WhenValidUserDetails() {
        // Given
        Token mockToken = createMockTokenWithUser();
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(mockToken));
        given(deleteUserPort.getLogoutCookies()).willReturn(logoutCookies);

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

            // 포트 호출 검증
            verify(loadTokenPort).findById(200L);
            verify(socialLoginPort).logout(SocialProvider.KAKAO, "mock-access-token");
            verify(deleteUserPort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("로그아웃 시 모든 의존성 호출 확인")
    void shouldCallAllDependencies_WhenLogout() {
        // Given
        Token mockToken = createMockTokenWithUser();
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(mockToken));
        given(deleteUserPort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            logoutService.logout(userDetails);

            // Then - 모든 의존성이 올바르게 호출되었는지 확인
            verify(loadTokenPort).findById(200L);
            verify(socialLoginPort).logout(SocialProvider.KAKAO, "mock-access-token");
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            verify(deleteUserPort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("토큰이 존재하지 않는 경우에도 로그아웃 성공")
    void shouldLogout_WhenTokenNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.empty());
        given(deleteUserPort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = logoutService.logout(userDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);
            verify(loadTokenPort).findById(200L);
            // 토큰이 없으면 socialLoginPort.logout은 호출되지 않음
            verify(socialLoginPort, never()).logout(any(SocialProvider.class), anyString());
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            verify(deleteUserPort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("토큰의 사용자가 null인 경우에도 로그아웃 성공")
    void shouldLogout_WhenTokenUserIsNull() {
        // Given
        Token tokenWithoutUser = mock(Token.class);
        given(tokenWithoutUser.getUsers()).willReturn(null);
        
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(loadTokenPort.findById(200L)).willReturn(Optional.of(tokenWithoutUser));
        given(deleteUserPort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = logoutService.logout(userDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);
            verify(loadTokenPort).findById(200L);
            // 사용자가 null이면 socialLoginPort.logout은 호출되지 않음
            verify(socialLoginPort, never()).logout(any(SocialProvider.class), anyString());
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            verify(deleteUserPort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }
}