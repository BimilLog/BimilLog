package jaeik.growfarm.integration;

import jaeik.growfarm.controller.UserController;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.dto.user.UserNameDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>UserController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 UserController의 전체 API를 테스트합니다.
 * </p>
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Commit
@Transactional
public class UserControllerIntegrationTest {

    private final UserController userController;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    private CustomUserDetails userDetails;

    public UserControllerIntegrationTest(UserController userController,
            SettingRepository settingRepository,
            TokenRepository tokenRepository,
            UserRepository userRepository) {
        this.userController = userController;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * <h3>테스트 데이터 초기화</h3>
     * 사용자 데이터 생성
     */
    @BeforeAll
    void setUp() {
        // 고유한 값 생성을 위한 timestamp
        long timestamp = System.currentTimeMillis();

        // 사용자 설정 생성
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        // 사용자 생성 (Token보다 먼저 생성해야 함)
        Users user = Users.builder()
                .kakaoId(timestamp) // 고유한 kakaoId
                .kakaoNickname("testNickname" + timestamp)
                .thumbnailImage("testImage")
                .userName("testUser" + timestamp) // 고유한 userName
                .role(UserRole.USER)
                .setting(setting)
                .build();
        Users testUser = userRepository.save(user);

        // 토큰 생성 (사용자 참조 포함)
        Token token = Token.builder()
                .users(testUser) // Users 참조 설정
                .jwtRefreshToken("testRefreshToken")
                .kakaoAccessToken("testKakaoAccessToken")
                .kakaoRefreshToken("testKakaoRefreshToken")
                .build();
        tokenRepository.save(token);

        // ClientDTO 생성
        ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
        userDetails = new CustomUserDetails(clientDTO);
    }

    @Test
    @DisplayName("내 게시글 목록 조회 테스트")
    void testGetMyPosts() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<Page<SimplePostDTO>> response = userController.getPostList(0, 10, userDetails);

        // Then
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("내 댓글 목록 조회 테스트")
    void testGetMyComments() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<Page<SimpleCommentDTO>> response = userController.getCommentList(0, 10, userDetails);

        // Then
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("설정 조회 테스트")
    void testGetSettings() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<SettingDTO> response = userController.getSetting(userDetails);

        // Then
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isMessageNotification());
    }

    @Test
    @DisplayName("닉네임 변경 테스트")
    void testUpdateUserName() {
        // Given
        UserNameDTO userNameDTO = new UserNameDTO();
        userNameDTO.setUserName("newTestUser");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<String> response = userController.updateUserName(userDetails, userNameDTO);

        // Then
        assertEquals("닉네임이 변경되었습니다.", response.getBody());
    }
}
