package jaeik.growfarm.integration;

import jaeik.growfarm.controller.AuthController;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for the AuthController.
 * These tests use the real database and services to test the full logic.
 */
@SpringBootTest
@Import(IntegrationTestConfig.class)
public class AuthControllerIntegrationTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private AuthService authService;

    /**
     * Test the health check endpoint.
     * This is a simple test to verify that the controller is working.
     */
    @Test
    @DisplayName("상태 검사 통합 테스트")
    void testHealthCheck() {
        // When
        ResponseEntity<String> response = authController.healthCheck();

        // Then
        assertEquals("OK", response.getBody());
    }

    /**
     * Test getting the current user.
     * This test creates a user and sets it in the security context,
     * then verifies that the controller returns the correct user information.
     */
    @Test
    @DisplayName("현재 로그인한 사용자 정보 조회 통합 테스트")
    void testGetCurrentUser() {
        // Given
        // Create a user for testing
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setFarmName("testFarm");
        userDTO.setKakaoNickname("testNickname");
        userDTO.setRole(UserRole.USER);

        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        // Set the user in the security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<?> response = authController.getCurrentUser(userDetails);

        // Then
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());
    }

    /**
     * This test demonstrates how to test with real database operations.
     * Note: This test is commented out because it would require a real Kakao login code,
     * which is not available in the test environment.
     */
    /*
    @Test
    @DisplayName("카카오 로그인 통합 테스트")
    void testLoginKakao() {
        // This would require a real Kakao login code
        // ResponseEntity<Object> response = authController.loginKakao("real-kakao-code");
        // assertNotNull(response.getBody());
    }
    */

    /**
     * This test demonstrates how to test with real database operations.
     * Note: This test is commented out because it would require a real token ID,
     * which is not available in the test environment.
     */
    /*
    @Test
    @DisplayName("회원가입 통합 테스트")
    void testSignUp() {
        // Given
        FarmNameReqDTO request = new FarmNameReqDTO();
        request.setTokenId(1L); // This would need to be a real token ID
        request.setFarmName("testFarm");

        // When
        ResponseEntity<Void> response = authController.SignUp(request);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }
    */
}
