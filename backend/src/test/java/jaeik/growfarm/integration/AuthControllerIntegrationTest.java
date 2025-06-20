package jaeik.growfarm.integration;

import jaeik.growfarm.controller.AuthController;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>AuthController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 AuthController의 전체 API를 테스트합니다.
 * </p>
 * <p>
 * 카카오 서버와 통신이 필요한 API는 테스트에서 제외함.
 * </p>
 * <p>
 * 이후에 카카오 Mock 서버를 만들어 테스트에 추가 필요.
 * </p>
 * 
 * @version 1.0.0
 * @author Jaeik
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Transactional
public class AuthControllerIntegrationTest {

    private final AuthController authController;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    private Users testUser;
    private Token testToken;
    private CustomUserDetails userDetails;
    private final Random random = new Random();

    public AuthControllerIntegrationTest(AuthController authController, SettingRepository settingRepository,
            TokenRepository tokenRepository, UserRepository userRepository) {
        this.authController = authController;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @BeforeAll
    void setUp() {
        // (deleteAll 호출 제거)

        // 고유한 값 생성
        int uniqueId = random.nextInt(1000000);
        long timestamp = System.currentTimeMillis();

        // 사용자 설정 생성
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        // 사용자 생성
        Users user = Users.builder()
                .kakaoId(timestamp + uniqueId)
                .kakaoNickname("testNickname" + uniqueId)
                .thumbnailImage("testImage")
                .userName("testUser" + uniqueId)
                .role(UserRole.USER)
                .setting(setting)
                .build();
        testUser = userRepository.save(user);

        // 토큰 생성
        testToken = Token.builder()
                .users(testUser)
                .jwtRefreshToken("testRefreshToken" + uniqueId)
                .kakaoAccessToken("testKakaoAccessToken" + uniqueId)
                .kakaoRefreshToken("testKakaoRefreshToken" + uniqueId)
                .build();
        testToken = tokenRepository.save(testToken);

        // ClientDTO 생성
        ClientDTO clientDTO = new ClientDTO(testUser, testToken.getId(), null);
        userDetails = new CustomUserDetails(clientDTO);
    }

    @AfterAll
    void tearDown() {
        // 별도 정리 로직 없음 (트랜잭션 롤백)
    }

    /**
     * <h3>서버 상태 검사 통합 테스트</h3>
     * 
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
     * 
     * @since 2025.05.17
     */
    @Test
    @DisplayName("현재 로그인한 사용자 정보 조회 통합 테스트")
    void testGetCurrentUser() {
        // Given
        SettingDTO settingDTO = new SettingDTO(testUser.getSetting());
        ClientDTO clientDTO = new ClientDTO(testUser, settingDTO, testToken.getId(), null);
        CustomUserDetails userDetails = new CustomUserDetails(clientDTO);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        // When
        ResponseEntity<?> response = authController.getCurrentUser(userDetails);

        // Then
        assertNotNull(response.getBody());
        assertEquals(clientDTO, response.getBody());
        assertEquals(clientDTO.getUserName(), ((ClientDTO) response.getBody()).getUserName());
        assertEquals(clientDTO.getKakaoId(), ((ClientDTO) response.getBody()).getKakaoId());
        assertEquals(clientDTO.getKakaoNickname(), ((ClientDTO) response.getBody()).getKakaoNickname());
        assertEquals(clientDTO.getThumbnailImage(), ((ClientDTO) response.getBody()).getThumbnailImage());
        assertEquals(clientDTO.getRole(), ((ClientDTO) response.getBody()).getRole());
        assertEquals(clientDTO.getSettingDTO(), ((ClientDTO) response.getBody()).getSettingDTO());
        assertEquals(clientDTO.getTokenId(), ((ClientDTO) response.getBody()).getTokenId());
        assertEquals(clientDTO.getUserId(), ((ClientDTO) response.getBody()).getUserId());
    }

    /*
     * 카카오 API가 필요한 테스트들은 Mock 서버 구축 후 추가 예정:
     * - testLoginKakao() - 카카오 로그인
     * - testSignUp() - 회원가입 (임시 쿠키 필요)
     * - testLogout() - 로그아웃 (카카오 로그아웃 필요)
     * - testWithdraw() - 회원탈퇴 (카카오 연결 해제 필요)
     */
}
