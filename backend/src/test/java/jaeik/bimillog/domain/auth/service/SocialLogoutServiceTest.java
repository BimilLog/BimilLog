package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialLogoutService;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>SocialLogoutService 단위 테스트</h2>
 * <p>로그아웃 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SocialLogoutService 단위 테스트")
class SocialLogoutServiceTest {

    @Mock
    private SocialStrategyRegistryPort strategyRegistry;

    @Mock
    private SocialStrategyPort kakaoStrategy;

    @Mock
    private GlobalTokenQueryPort globalTokenQueryPort;

    @Mock
    private GlobalCookiePort globalCookiePort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private SocialLogoutService socialLogoutService;

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
        given(mockToken.getAccessToken()).willReturn("mock-access-TemporaryToken");
        
        return mockToken;
    }

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldLogout_WhenValidUserDetails() {
        // Given
        Token mockToken = createMockTokenWithUser();
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(userDetails.getSocialProvider()).willReturn(SocialProvider.KAKAO);
        given(globalTokenQueryPort.findById(200L)).willReturn(Optional.of(mockToken));
        given(strategyRegistry.getStrategy(SocialProvider.KAKAO)).willReturn(kakaoStrategy);
        given(globalCookiePort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = socialLogoutService.logout(userDetails);

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
            verify(globalTokenQueryPort).findById(200L);
            verify(strategyRegistry).getStrategy(SocialProvider.KAKAO);
            verify(kakaoStrategy).logout(SocialProvider.KAKAO, "mock-access-TemporaryToken");
            verify(globalCookiePort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }


    @Test
    @DisplayName("토큰이 존재하지 않는 경우 AuthCustomException 발생")
    void shouldThrowException_WhenTokenNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(globalTokenQueryPort.findById(200L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> socialLogoutService.logout(userDetails))
            .isInstanceOf(AuthCustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.NOT_FIND_TOKEN);

        // 예외 발생으로 다른 메서드들은 호출되지 않음
        verify(globalTokenQueryPort).findById(200L);
        verify(strategyRegistry, never()).getStrategy(any(SocialProvider.class));
        verify(kakaoStrategy, never()).logout(any(SocialProvider.class), anyString());
        verify(eventPublisher, never()).publishEvent(any(UserLoggedOutEvent.class));
        verify(globalCookiePort, never()).getLogoutCookies();
    }

    @Test
    @DisplayName("소셜 로그아웃 실패시에도 전체 로그아웃은 성공")
    void shouldCompleteLogout_WhenSocialLogoutFails() throws Exception {
        // Given
        Token mockToken = createMockTokenWithUser();
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(userDetails.getSocialProvider()).willReturn(SocialProvider.KAKAO);
        given(globalTokenQueryPort.findById(200L)).willReturn(Optional.of(mockToken));
        given(strategyRegistry.getStrategy(SocialProvider.KAKAO)).willReturn(kakaoStrategy);
        // 소셜 로그아웃 실패
        doThrow(new RuntimeException("Kakao logout failed"))
            .when(kakaoStrategy).logout(any(SocialProvider.class), anyString());
        given(globalCookiePort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = socialLogoutService.logout(userDetails);

            // Then - 소셜 로그아웃 실패해도 전체 로그아웃은 성공
            assertThat(result).isEqualTo(logoutCookies);
            verify(globalTokenQueryPort).findById(200L);
            verify(strategyRegistry).getStrategy(SocialProvider.KAKAO);
            verify(kakaoStrategy).logout(SocialProvider.KAKAO, "mock-access-TemporaryToken");
            // 이벤트는 정상 발행됨
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            verify(globalCookiePort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }
}