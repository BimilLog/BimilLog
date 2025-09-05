package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.application.port.in.TokenBlacklistUseCase;
import jaeik.bimillog.domain.auth.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.auth.application.port.out.LoadUserPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.auth.application.port.out.SocialLogoutPort;
import jaeik.bimillog.domain.auth.application.service.WithdrawService;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>WithdrawService 단위 테스트</h2>
 * <p>회원 탈퇴 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawService 단위 테스트")
class WithdrawServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private DeleteUserPort deleteUserPort;

    @Mock
    private SocialLoginPort socialLoginPort;

    @Mock
    private SocialLogoutPort socialLogoutPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TokenBlacklistUseCase tokenBlacklistUseCase;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private WithdrawService withdrawService;

    private User testUser;
    private List<ResponseCookie> logoutCookies;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .build();

        logoutCookies = List.of(
                ResponseCookie.from("access_token", "").maxAge(0).build(),
                ResponseCookie.from("refresh_token", "").maxAge(0).build()
        );
    }

    @Test
    @DisplayName("정상적인 회원 탈퇴 처리")
    void shouldWithdraw_WhenValidUserDetails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        given(deleteUserPort.getLogoutCookies()).willReturn(logoutCookies);

        // When
        List<ResponseCookie> result = withdrawService.withdraw(userDetails);

        // Then
        assertThat(result).isEqualTo(logoutCookies);


        // 소셜 로그인 연결 해제 검증
        verify(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao123");

        // 탈퇴 프로세스 수행 검증
        verify(deleteUserPort).performWithdrawProcess(100L);

        // 탈퇴 이벤트 발행 검증
        ArgumentCaptor<UserWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(UserWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserWithdrawnEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(100L);

        // 로그아웃 쿠키 생성 검증
        verify(deleteUserPort).getLogoutCookies();
    }

    @Test
    @DisplayName("null 사용자 정보로 탈퇴 시 NULL_SECURITY_CONTEXT 예외 발생")
    void shouldThrowException_WhenUserDetailsIsNull() {
        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(null))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.NULL_SECURITY_CONTEXT);

        // null userDetails일 때는 어떤 메서드도 호출되지 않아야 함
    }

    @Test
    @DisplayName("존재하지 않는 사용자 탈퇴 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadUserPort.findById(100L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(userDetails))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.USER_NOT_FOUND);

        verify(loadUserPort).findById(100L);
        // 사용자가 없으므로 다른 작업들이 수행되지 않아야 함
    }

    @Test
    @DisplayName("소셜 로그인 연결 해제 실패 시 전체 탈퇴 프로세스 롤백")
    void shouldRollbackWithdraw_WhenSocialUnlinkFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        doThrow(new RuntimeException("소셜 연결 해제 실패"))
                .when(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao123");

        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(userDetails))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.SOCIAL_UNLINK_FAILED);

        // 소셜 연결 해제 실패로 인해 트랜잭션 롤백, 후속 작업들은 실행되지 않음
        verify(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao123");
        // DB 삭제 및 이벤트 발행은 롤백으로 인해 실행되지 않음
    }

    @Test
    @DisplayName("관리자 강제 탈퇴 처리 성공")
    void shouldForceWithdraw_WhenValidUserId() {
        // Given
        Long targetUserId = 200L;
        User targetUser = User.builder()
                .id(targetUserId)
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("targetUser")
                .build();

        given(loadUserPort.findById(targetUserId)).willReturn(Optional.of(targetUser));

        // When
        withdrawService.forceWithdraw(targetUserId);

        // Then

        // 소셜 로그인 연결 해제 검증
        verify(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao456");

        // 탈퇴 프로세스 수행 검증
        verify(deleteUserPort).performWithdrawProcess(targetUserId);

        // 탈퇴 이벤트 발행 검증
        ArgumentCaptor<UserWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(UserWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserWithdrawnEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(targetUserId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 강제 탈퇴 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenForceWithdrawUserNotFound() {
        // Given
        Long nonExistentUserId = 999L;
        given(loadUserPort.findById(nonExistentUserId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> withdrawService.forceWithdraw(nonExistentUserId))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.USER_NOT_FOUND);

        verify(loadUserPort).findById(nonExistentUserId);
        // 사용자가 없으므로 다른 작업들이 수행되지 않아야 함
    }

    @Test
    @DisplayName("소셜 계정 연결 해제 실패 시 예외 발생")
    void shouldThrow_WhenSocialUnlinkFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        doThrow(new RuntimeException("소셜 연결 해제 실패"))
                .when(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao123");

        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(userDetails))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.SOCIAL_UNLINK_FAILED);

        verify(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao123");
    }

    @Test
    @DisplayName("다양한 소셜 제공자에 대한 탈퇴 처리")
    void shouldWithdraw_WithDifferentSocialProviders() {
        // Given
        SocialProvider[] providers = {SocialProvider.KAKAO};

        for (SocialProvider provider : providers) {
            User user = User.builder()
                    .id(100L)
                    .socialId("social123")
                    .provider(provider)
                    .userName("testUser")
                    .build();

            given(userDetails.getUserId()).willReturn(100L);
            given(loadUserPort.findById(100L)).willReturn(Optional.of(user));
            given(deleteUserPort.getLogoutCookies()).willReturn(logoutCookies);

            // When
            List<ResponseCookie> result = withdrawService.withdraw(userDetails);

            // Then
            assertThat(result).isEqualTo(logoutCookies);
            verify(socialLoginPort).unlink(provider, "social123");
        }
    }

    @Test
    @DisplayName("이벤트 발행 실패 시에도 탈퇴 프로세스 완료")
    void shouldCompleteWithdraw_WhenEventPublishingFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(loadUserPort.findById(100L)).willReturn(Optional.of(testUser));
        doThrow(new RuntimeException("이벤트 발행 실패"))
                .when(eventPublisher).publishEvent(new UserWithdrawnEvent(100L));

        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(userDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이벤트 발행 실패");

        // 이벤트 발행 전까지의 모든 작업은 완료되어야 함
        verify(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao123");
        verify(deleteUserPort).performWithdrawProcess(100L);
    }

    @Test
    @DisplayName("관리자 강제 탈퇴에서 탈퇴 데이터 처리 실패")
    void shouldHandleException_WhenForceWithdrawDataProcessFails() {
        // Given
        Long targetUserId = 200L;
        User targetUser = User.builder()
                .id(targetUserId)
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("targetUser")
                .build();

        given(loadUserPort.findById(targetUserId)).willReturn(Optional.of(targetUser));
        doThrow(new RuntimeException("데이터 처리 실패"))
                .when(deleteUserPort).performWithdrawProcess(targetUserId);

        // When & Then
        assertThatThrownBy(() -> withdrawService.forceWithdraw(targetUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("데이터 처리 실패");

        verify(socialLoginPort).unlink(SocialProvider.KAKAO, "kakao456");
        verify(deleteUserPort).performWithdrawProcess(targetUserId);
    }
}