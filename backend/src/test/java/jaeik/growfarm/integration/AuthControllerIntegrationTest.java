package jaeik.growfarm.integration;

import jaeik.growfarm.controller.AuthController;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>AuthController 통합 테스트</h2>
 * <p>실제 데이터베이스와 서비스를 사용하여 AuthController의 전체 API를 테스트합니다.</p>
 * <p>카카오 서버와 통신이 필요한 API는 테스트에서 제외함.</p>
 * <p>이후에 카카오 Mock 서버를 만들어 테스트에 추가 필요.</p>
 * @since 2025.05.17
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Commit
@Transactional
public class AuthControllerIntegrationTest {

    private final AuthController authController;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;

    private Users testUser;

    public AuthControllerIntegrationTest(AuthController authController, SettingRepository settingRepository, TokenRepository tokenRepository, UserRepository userRepository, UserUtil userUtil) {
        this.authController = authController;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userUtil = userUtil;
    }

    /**
     * <h3>테스트 데이터 초기화</h3>
     * 사용자 데이터 생성
     *
     * @since 2025.05.17
     */
    @BeforeAll
    void setUp() {
        Setting setting = Setting.builder()
                .farmNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .commentFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        Users user = Users.builder()
                .kakaoId(1234567890L)
                .kakaoNickname("testNickname")
                .thumbnailImage("testImage")
                .farmName("testFarm")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        testUser = userRepository.save(user);

        Token token = Token.builder()
                .jwtRefreshToken("testRefreshToken")
                .kakaoAccessToken("testKakaoAccessToken")
                .kakaoRefreshToken("testKakaoRefreshToken")
                .users(testUser)
                .build();
        tokenRepository.save(token);

    }

    /**
     * <h3>서버 상태 검사 통합 테스트</h3>
     * @since 2025.05.17
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
     * <h3>현재 로그인한 사용자 정보 조회 통합 테스트</h3>
     * @since 2025.05.17
     */
    @Test
    @DisplayName("현재 로그인한 사용자 정보 조회 통합 테스트")
    void testGetCurrentUser() {
        // Given
        UserDTO userDTO = userUtil.UserToDTO(testUser);
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
        // When
        ResponseEntity<?> response = authController.getCurrentUser(userDetails);

        // Then
        assertNotNull(response.getBody());
        assertEquals(userDTO, response.getBody());
        assertEquals(userDTO.getFarmName(), ((UserDTO) response.getBody()).getFarmName());
        assertEquals(userDTO.getKakaoId(), ((UserDTO) response.getBody()).getKakaoId());
        assertEquals(userDTO.getKakaoNickname(), ((UserDTO) response.getBody()).getKakaoNickname());
        assertEquals(userDTO.getThumbnailImage(), ((UserDTO) response.getBody()).getThumbnailImage());
        assertEquals(userDTO.getRole(), ((UserDTO) response.getBody()).getRole());
        assertEquals(userDTO.getSetting(), ((UserDTO) response.getBody()).getSetting());
        assertEquals(userDTO.getTokenId(), ((UserDTO) response.getBody()).getTokenId());
        assertEquals(userDTO.getUserId(), ((UserDTO) response.getBody()).getUserId());
    }
}
