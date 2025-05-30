package jaeik.growfarm.controller;

import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

//    @Test
//    @DisplayName("카카오 로그인 테스트 - 기존 회원")
//    void testLoginKakaoExistingUser() {
//        // Given
//        List<ResponseCookie> cookies = new ArrayList<>();
//        cookies.add(ResponseCookie.from("accessToken", "test-access-token").build());
//        cookies.add(ResponseCookie.from("refreshToken", "test-refresh-token").build());
//
//        when(authService.processKakaoLogin(anyString())).thenReturn(cookies);
//
//        // When
//        ResponseEntity<Object> response = authController.loginKakao("test-code");
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
//        assertEquals(2, Objects.requireNonNull(response.getHeaders().get("Set-Cookie")).size());
//    }
//
//    @Test
//    @DisplayName("카카오 로그인 테스트 - 신규 회원")
//    void testLoginKakaoNewUser() {
//        // Given
//        Long tokenId = 1L;
//        when(authService.processKakaoLogin(anyString())).thenReturn(tokenId);
//
//        // When
//        ResponseEntity<Object> response = authController.loginKakao("test-code");
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(tokenId, response.getBody());
//    }
//
//    @Test
//    @DisplayName("회원가입 테스트")
//    void testSignUp() {
//        // Given
//        FarmNameReqDTO request = new FarmNameReqDTO();
//        request.setTokenId(1L);
//        request.setFarmName("testFarm");
//
//        List<ResponseCookie> cookies = new ArrayList<>();
//        cookies.add(ResponseCookie.from("accessToken", "test-access-token").build());
//        cookies.add(ResponseCookie.from("refreshToken", "test-refresh-token").build());
//
//        when(authService.signUp(any(), anyString())).thenReturn(cookies);
//
//        // When
//        ResponseEntity<Void> response = authController.SignUp(request);
//
//        // Then
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
//        assertEquals(2, response.getHeaders().get("Set-Cookie").size());
//    }

    @Test
    @DisplayName("로그아웃 테스트")
    void testLogout() {
        // Given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        List<ResponseCookie> cookies = new ArrayList<>();
        cookies.add(ResponseCookie.from("accessToken", "").maxAge(0).build());
        cookies.add(ResponseCookie.from("refreshToken", "").maxAge(0).build());

        when(authService.logout(any())).thenReturn(cookies);

        // When
        ResponseEntity<String> response = authController.logout(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("로그아웃 완료", response.getBody());
        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
        assertEquals(2, response.getHeaders().get("Set-Cookie").size());
    }

    @Test
    @DisplayName("회원 탈퇴 테스트")
    void testWithdraw() {
        // Given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        List<ResponseCookie> cookies = new ArrayList<>();
        cookies.add(ResponseCookie.from("accessToken", "").maxAge(0).build());
        cookies.add(ResponseCookie.from("refreshToken", "").maxAge(0).build());

        when(authService.withdraw(any())).thenReturn(cookies);

        // When
        ResponseEntity<?> response = authController.withdraw(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("회원탈퇴 완료", response.getBody());
        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
        assertEquals(2, response.getHeaders().get("Set-Cookie").size());
    }

    @Test
    @DisplayName("현재 로그인한 사용자 정보 조회 테스트")
    void testGetCurrentUser() {
        // Given
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setFarmName("testFarm");
        userDTO.setKakaoNickname("testNickname");
        userDTO.setRole(UserRole.USER);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserDTO()).thenReturn(userDTO);

        // When
        ResponseEntity<?> response = authController.getCurrentUser(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDTO, response.getBody());
    }

    @Test
    @DisplayName("상태 검사 테스트")
    void testHealthCheck() {
        // When
        ResponseEntity<String> response = authController.healthCheck();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
    }
}
