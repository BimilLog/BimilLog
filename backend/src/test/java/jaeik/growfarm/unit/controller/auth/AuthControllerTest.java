package jaeik.growfarm.unit.controller.auth;

import jaeik.growfarm.controller.AuthController;
import jaeik.growfarm.service.auth.AuthService;
import jaeik.growfarm.service.auth.AuthUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * <h2>AuthController 단위 테스트</h2>
 * <p>
 * AuthController의 각 메서드를 단위 테스트로 검증합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Mock
    private AuthUpdateService authUpdateService;

    @Test
    @DisplayName("헬스체크 테스트")
    void testHealthCheck() {
        // When
        ResponseEntity<String> response = authController.healthCheck();

        // Then
        assertNotNull(response);
    }
}