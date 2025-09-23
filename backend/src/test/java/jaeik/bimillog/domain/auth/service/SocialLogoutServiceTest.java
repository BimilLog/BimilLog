package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyRegistryPort;
import jaeik.bimillog.domain.auth.application.service.SocialLogoutService;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.global.application.port.out.GlobalTokenQueryPort;
import jaeik.bimillog.testutil.BaseAuthUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
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
@DisplayName("SocialLogoutService 단위 테스트")
class SocialLogoutServiceTest extends BaseAuthUnitTest {

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

    @InjectMocks
    private SocialLogoutService socialLogoutService;

    @BeforeEach
    void setUp() {
        // BaseAuthUnitTest의 logoutCookies를 mock에서 반환하도록 설정
        given(globalCookiePort.getLogoutCookies()).willReturn(logoutCookies);
    }

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldLogout_WhenValidUserDetails() {
        // Given
        Token mockToken = createMockTokenWithUser(testUser);
        given(testCustomUserDetails.getUserId()).willReturn(100L);
        given(testCustomUserDetails.getTokenId()).willReturn(200L);
        given(testCustomUserDetails.getSocialProvider()).willReturn(TEST_PROVIDER);
        given(globalTokenQueryPort.findById(200L)).willReturn(Optional.of(mockToken));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = socialLogoutService.logout(testCustomUserDetails);

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
            verify(strategyRegistry).getStrategy(TEST_PROVIDER);
            try {
                verify(kakaoStrategy).logout(TEST_PROVIDER, TEST_ACCESS_TOKEN);
            } catch (Exception e) {
                // verify 호출 시 예외는 무시
            }
            verify(globalCookiePort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("토큰이 존재하지 않는 경우 예외 발생")
    void shouldThrowException_WhenTokenNotFound() {
        // Given
        given(testCustomUserDetails.getUserId()).willReturn(100L);
        given(testCustomUserDetails.getTokenId()).willReturn(200L);
        given(globalTokenQueryPort.findById(200L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> socialLogoutService.logout(testCustomUserDetails))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.NOT_FIND_TOKEN);

        verify(globalTokenQueryPort).findById(200L);
        // 토큰이 없으면 예외가 발생하므로 다른 메서드들은 호출되지 않음
        verify(strategyRegistry, never()).getStrategy(any(SocialProvider.class));
        verifyNoMoreInteractions(kakaoStrategy);
        verify(eventPublisher, never()).publishEvent(any());
        verify(globalCookiePort, never()).getLogoutCookies();
    }



    @Test
    @DisplayName("소셜 로그아웃 실패 시에도 전체 로그아웃 프로세스는 성공")
    void shouldCompleteLogout_WhenSocialLogoutFails() {
        // Given
        Token mockToken = createMockTokenWithUser(testUser);
        given(testCustomUserDetails.getUserId()).willReturn(100L);
        given(testCustomUserDetails.getTokenId()).willReturn(200L);
        given(testCustomUserDetails.getSocialProvider()).willReturn(TEST_PROVIDER);
        given(globalTokenQueryPort.findById(200L)).willReturn(Optional.of(mockToken));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);
        
        // 소셜 로그아웃이 실패하도록 설정
        try {
            doThrow(new RuntimeException("Social logout failed"))
                .when(kakaoStrategy).logout(TEST_PROVIDER, TEST_ACCESS_TOKEN);
        } catch (Exception e) {
            // mock 설정 중 예외는 무시
        }

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = socialLogoutService.logout(testCustomUserDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);
            
            // 소셜 로그아웃이 실패해도 다른 프로세스는 정상 실행되어야 함
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            verify(globalCookiePort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("다양한 사용자 정보로 로그아웃 처리")
    void shouldHandleDifferentUserDetails() {
        // Given - 관리자 사용자
        Token adminToken = createMockTokenWithUser(adminUser);
        
        given(testCustomUserDetails.getUserId()).willReturn(999L);
        given(testCustomUserDetails.getTokenId()).willReturn(888L);
        given(testCustomUserDetails.getSocialProvider()).willReturn(TEST_PROVIDER);
        given(globalTokenQueryPort.findById(888L)).willReturn(Optional.of(adminToken));
        given(strategyRegistry.getStrategy(TEST_PROVIDER)).willReturn(kakaoStrategy);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            List<ResponseCookie> result = socialLogoutService.logout(testCustomUserDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);
            
            ArgumentCaptor<UserLoggedOutEvent> eventCaptor = ArgumentCaptor.forClass(UserLoggedOutEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            
            UserLoggedOutEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.userId()).isEqualTo(999L);
            assertThat(capturedEvent.tokenId()).isEqualTo(888L);
        }
    }


}