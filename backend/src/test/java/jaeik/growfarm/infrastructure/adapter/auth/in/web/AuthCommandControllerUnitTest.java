package jaeik.growfarm.infrastructure.adapter.auth.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.growfarm.domain.auth.entity.LoginResult;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * <h2>인증 명령 컨트롤러 단위 테스트</h2>
 * <p>AuthCommandController의 비즈니스 로직 단위 테스트</p>
 * <p>@ExtendWith(MockitoExtension.class)를 사용한 순수 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("인증 명령 컨트롤러 단위 테스트")
class AuthCommandControllerUnitTest {

    @Mock
    private SocialLoginUseCase socialLoginUseCase;

    @Mock
    private SignUpUseCase signUpUseCase;

    @Mock
    private LogoutUseCase logoutUseCase;

    @Mock
    private WithdrawUseCase withdrawUseCase;

    @InjectMocks
    private AuthCommandController authCommandController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("소셜 로그인 - 신규 사용자 성공")
    void socialLogin_NewUser_Success() {
        // Given
        String provider = "KAKAO";
        String code = "test-auth-code";
        String fcmToken = "test-fcm-token";
        String uuid = "test-uuid";
        ResponseCookie tempCookie = ResponseCookie.from("temp", "temp-value")
                .maxAge(Duration.ofMinutes(30))
                .httpOnly(true)
                .build();

        LoginResult.NewUser loginResult = new LoginResult.NewUser(uuid, tempCookie);
        given(socialLoginUseCase.processSocialLogin(eq(SocialProvider.KAKAO), eq(code), eq(fcmToken)))
                .willReturn(loginResult);

        // When
        ResponseEntity<?> response = authCommandController.socialLogin(provider, code, fcmToken);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().get("Set-Cookie")).contains(tempCookie.toString());
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsEntry("uuid", uuid);
        
        verify(socialLoginUseCase).processSocialLogin(eq(SocialProvider.KAKAO), eq(code), eq(fcmToken));
    }

    @Test
    @DisplayName("소셜 로그인 - 기존 사용자 성공")
    void socialLogin_ExistingUser_Success() {
        // Given
        String provider = "KAKAO";
        String code = "test-auth-code";
        ResponseCookie accessCookie = ResponseCookie.from("access", "access-token")
                .maxAge(Duration.ofHours(1))
                .httpOnly(true)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh", "refresh-token")
                .maxAge(Duration.ofDays(7))
                .httpOnly(true)
                .build();

        LoginResult.ExistingUser loginResult = new LoginResult.ExistingUser(
                List.of(accessCookie, refreshCookie));
        given(socialLoginUseCase.processSocialLogin(eq(SocialProvider.KAKAO), eq(code), eq((String) null)))
                .willReturn(loginResult);

        // When
        ResponseEntity<?> response = authCommandController.socialLogin(provider, code, null);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().get("Set-Cookie"))
                .containsExactlyInAnyOrder(accessCookie.toString(), refreshCookie.toString());
        assertThat(response.getBody()).isEqualTo("OK");
        
        verify(socialLoginUseCase).processSocialLogin(eq(SocialProvider.KAKAO), eq(code), eq((String) null));
    }

    @Test
    @DisplayName("소셜 로그인 - 잘못된 provider 파라미터")
    void socialLogin_InvalidProvider_ThrowsException() {
        // Given
        String provider = "INVALID_PROVIDER";
        String code = "test-auth-code";

        // When & Then
        assertThatThrownBy(() -> authCommandController.socialLogin(provider, code, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("소셜 로그인 - 서비스 예외 발생")
    void socialLogin_ServiceException_ThrowsException() {
        // Given
        String provider = "KAKAO";
        String code = "invalid-code";
        
        given(socialLoginUseCase.processSocialLogin(eq(SocialProvider.KAKAO), eq(code), eq((String) null)))
                .willThrow(new CustomException(ErrorCode.KAKAO_API_ERROR));

        // When & Then
        assertThatThrownBy(() -> authCommandController.socialLogin(provider, code, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.KAKAO_API_ERROR.getMessage());
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void signUp_Success() {
        // Given
        String userName = "테스트사용자";
        String uuid = "test-uuid";
        ResponseCookie accessCookie = ResponseCookie.from("access", "access-token")
                .maxAge(Duration.ofHours(1))
                .httpOnly(true)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh", "refresh-token")
                .maxAge(Duration.ofDays(7))
                .httpOnly(true)
                .build();

        given(signUpUseCase.signUp(eq(userName), eq(uuid)))
                .willReturn(List.of(accessCookie, refreshCookie));

        // When
        ResponseEntity<?> response = authCommandController.signUp(userName, uuid);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().get("Set-Cookie"))
                .containsExactlyInAnyOrder(accessCookie.toString(), refreshCookie.toString());
        assertThat(response.getBody()).isEqualTo("OK");
        
        verify(signUpUseCase).signUp(eq(userName), eq(uuid));
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_Success() {
        // Given
        CustomUserDetails userDetails = createMockUserDetails(1L);
        ResponseCookie logoutCookie = ResponseCookie.from("access", "")
                .maxAge(Duration.ZERO)
                .httpOnly(true)
                .build();

        given(logoutUseCase.logout(any(CustomUserDetails.class)))
                .willReturn(List.of(logoutCookie));

        // When
        ResponseEntity<?> response = authCommandController.logout(userDetails);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().get("Set-Cookie")).contains(logoutCookie.toString());
        assertThat(response.getBody()).isEqualTo("OK");
        
        verify(logoutUseCase).logout(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("회원탈퇴 - 성공")
    void withdraw_Success() {
        // Given
        CustomUserDetails userDetails = createMockUserDetails(1L);
        ResponseCookie withdrawCookie = ResponseCookie.from("access", "")
                .maxAge(Duration.ZERO)
                .httpOnly(true)
                .build();

        given(withdrawUseCase.withdraw(any(CustomUserDetails.class)))
                .willReturn(List.of(withdrawCookie));

        // When
        ResponseEntity<?> response = authCommandController.withdraw(userDetails);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().get("Set-Cookie")).contains(withdrawCookie.toString());
        assertThat(response.getBody()).isEqualTo("OK");
        
        verify(withdrawUseCase).withdraw(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("회원탈퇴 - 서비스 예외 발생")
    void withdraw_ServiceException_ThrowsException() {
        // Given
        CustomUserDetails userDetails = createMockUserDetails(1L);
        
        willThrow(new CustomException(ErrorCode.NOT_FOUND_USER))
                .given(withdrawUseCase).withdraw(any(CustomUserDetails.class));

        // When & Then
        assertThatThrownBy(() -> authCommandController.withdraw(userDetails))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_FOUND_USER.getMessage());
    }

    @Test
    @DisplayName("로그인 결과 변환 - 신규 사용자")
    void convertToLoginResponse_NewUser() {
        // Given
        String uuid = "test-uuid";
        ResponseCookie tempCookie = ResponseCookie.from("temp", "temp-value").build();
        LoginResult.NewUser domainResult = new LoginResult.NewUser(uuid, tempCookie);
        
        given(socialLoginUseCase.processSocialLogin(eq(SocialProvider.KAKAO), eq("code"), eq((String) null)))
                .willReturn(domainResult);

        // When
        ResponseEntity<?> response = authCommandController.socialLogin("KAKAO", "code", null);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsEntry("uuid", uuid);
    }

    @Test
    @DisplayName("로그인 결과 변환 - 기존 사용자")
    void convertToLoginResponse_ExistingUser() {
        // Given
        ResponseCookie cookie = ResponseCookie.from("access", "token").build();
        LoginResult.ExistingUser domainResult = new LoginResult.ExistingUser(List.of(cookie));
        
        given(socialLoginUseCase.processSocialLogin(eq(SocialProvider.KAKAO), eq("code"), eq((String) null)))
                .willReturn(domainResult);

        // When
        ResponseEntity<?> response = authCommandController.socialLogin("KAKAO", "code", null);

        // Then
        assertThat(response.getBody()).isEqualTo("OK");
        assertThat(response.getHeaders().get("Set-Cookie")).contains(cookie.toString());
    }

    /**
     * 테스트용 Mock CustomUserDetails 생성
     */
    private CustomUserDetails createMockUserDetails(Long userId) {
        UserDTO userDTO = UserDTO.builder()
                .userId(userId)
                .settingId(100L)
                .socialId("social123")
                .socialNickname("테스트사용자")
                .thumbnailImage("http://example.com/image.jpg")
                .userName("테스트사용자")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .build();
        
        return new CustomUserDetails(userDTO);
    }
}