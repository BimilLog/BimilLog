package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.out.DeleteUserPort;
import jaeik.growfarm.domain.auth.application.port.out.SocialLogoutPort;
import jaeik.growfarm.domain.auth.event.UserLoggedOutEvent;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
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
//TODO 비즈니스 로직의 변경으로 테스트코드와 비즈니스 로직의 흐름이 맞지 않을 시 테스트 코드의 변경이 적으면 테스트 수정 필요 변경이 많으면 Deprecated 처리 후 새로운 단위 테스트 작성 필요
@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutService 단위 테스트")
class LogoutServiceTest {

    @Mock
    private DeleteUserPort deleteUserPort;

    @Mock
    private SocialLogoutPort socialLogoutPort;

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

    @Test
    @DisplayName("정상적인 로그아웃 처리")
    void shouldLogout_WhenValidUserDetails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
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
            verify(socialLogoutPort).performSocialLogout(userDetails);
            verify(deleteUserPort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("로그아웃 시 모든 의존성 호출 확인")
    void shouldCallAllDependencies_WhenLogout() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userDetails.getTokenId()).willReturn(200L);
        given(deleteUserPort.getLogoutCookies()).willReturn(logoutCookies);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            // When
            logoutService.logout(userDetails);

            // Then - 모든 의존성이 올바르게 호출되었는지 확인
            verify(socialLogoutPort).performSocialLogout(userDetails);
            verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
            verify(deleteUserPort).getLogoutCookies();
            mockedSecurityContext.verify(SecurityContextHolder::clearContext);
        }
    }
}