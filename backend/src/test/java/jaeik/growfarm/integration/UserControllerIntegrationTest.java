package jaeik.growfarm.integration;

import jaeik.growfarm.controller.UserController;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.dto.user.UserNameDTO;
import jaeik.growfarm.entity.report.ReportType;
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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>UserController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 UserController의 전체 API를 테스트합니다.
 * </p>
 * <p>
 * 카카오 서버와 통신이 필요한 API는 테스트에서 제외함.
 * </p>
 * <p>
 * 이후에 카카오 Mock 서버를 만들어 테스트에 추가 필요.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Transactional
public class UserControllerIntegrationTest {

        private final UserController userController;
        private final UserRepository userRepository;
        private final SettingRepository settingRepository;
        private final TokenRepository tokenRepository;

        private CustomUserDetails userDetails;
        private final Random random = new Random();

        public UserControllerIntegrationTest(UserController userController,
                        UserRepository userRepository,
                        SettingRepository settingRepository,
                        TokenRepository tokenRepository) {
                this.userController = userController;
                this.userRepository = userRepository;
                this.settingRepository = settingRepository;
                this.tokenRepository = tokenRepository;
        }

        @BeforeAll
        void setUp() {
                // 고유한 값 생성을 위한 랜덤 값
                int uniqueId = random.nextInt(1000000);
                long timestamp = System.currentTimeMillis();

                // 사용자 설정 생성
                Setting setting = Setting.builder()
                                .messageNotification(true)
                                .commentNotification(true)
                                .postFeaturedNotification(true)
                                .build();
                settingRepository.save(setting);

                // 사용자 생성 (고유한 값 사용)
                Users user = Users.builder()
                                .kakaoId(timestamp + uniqueId) // 고유한 kakaoId
                                .kakaoNickname("testNickname" + uniqueId)
                                .thumbnailImage("testImage")
                                .userName("testUser" + uniqueId) // 고유한 userName
                                .role(UserRole.USER)
                                .setting(setting)
                                .build();
                Users testUser = userRepository.save(user);

                // 토큰 생성 (사용자 참조 포함)
                Token token = Token.builder()
                                .users(testUser) // Users 참조 설정
                                .jwtRefreshToken("testRefreshToken" + uniqueId)
                                .kakaoAccessToken("testKakaoAccessToken" + uniqueId)
                                .kakaoRefreshToken("testKakaoRefreshToken" + uniqueId)
                                .build();
                tokenRepository.save(token);

                // ClientDTO 생성
                ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
                userDetails = new CustomUserDetails(clientDTO);
        }

        @Test
        @Order(1)
        @DisplayName("내 게시글 목록 조회 테스트")
        void testGetMyPosts() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<Page<SimplePostDTO>> response = userController.getPostList(0, 10, userDetails);

                // Then
                assertNotNull(response.getBody());
        }

        @Test
        @Order(2)
        @DisplayName("내 댓글 목록 조회 테스트")
        void testGetMyComments() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<Page<SimpleCommentDTO>> response = userController.getCommentList(0, 10, userDetails);

                // Then
                assertNotNull(response.getBody());
        }

        @Test
        @Order(3)
        @DisplayName("설정 조회 테스트")
        void testGetSettings() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<SettingDTO> response = userController.getSetting(userDetails);

                // Then
                assertNotNull(response.getBody());
                assertTrue(response.getBody().isMessageNotification());
        }

        @Test
        @Order(4)
        @DisplayName("닉네임 변경 테스트")
        void testUpdateUserName() {
                // Given
                int uniqueId = random.nextInt(1000000);
                UserNameDTO userNameDTO = new UserNameDTO();
                userNameDTO.setUserName("newTestUser" + uniqueId); // 고유한 닉네임

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<String> response = userController.updateUserName(userDetails, userNameDTO);

                // Then
                assertEquals("닉네임이 변경되었습니다.", response.getBody());
        }

        @Test
        @DisplayName("사용자가 추천한 게시글 목록 조회 통합 테스트")
        void testGetLikedPosts() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<Page<SimplePostDTO>> response = userController.getLikedPosts(0, 10, userDetails);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("사용자가 추천한 댓글 목록 조회 통합 테스트")
        void testGetLikedComments() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<Page<SimpleCommentDTO>> response = userController.getLikedComments(0, 10, userDetails);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("건의하기 통합 테스트")
        void testSuggestion() {
                // Given
                int uniqueId = random.nextInt(1000000);
                ReportDTO reportDTO = ReportDTO.builder()
                                .reportType(ReportType.IMPROVEMENT)
                                .content("건의사항 테스트 " + uniqueId)
                                .build();

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<String> response = userController.suggestion(userDetails, reportDTO);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertEquals("건의가 접수되었습니다.", response.getBody());
        }

        @Test
        @DisplayName("사용자 설정 조회 통합 테스트")
        void testGetSetting() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<SettingDTO> response = userController.getSetting(userDetails);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("사용자 설정 수정 통합 테스트")
        void testUpdateSetting() {
                // Given
                SettingDTO settingDTO = new SettingDTO(1L, false, true, false);
                settingDTO.setMessageNotification(false);
                settingDTO.setCommentNotification(true);
                settingDTO.setPostFeaturedNotification(false);

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<String> response = userController.updateSetting(settingDTO, userDetails);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertEquals("설정 수정 완료", response.getBody());
        }

        /*
         * 카카오 API가 필요한 테스트는 Mock 서버 구축 후 추가 예정:
         * - testGetFriendList() - 카카오 친구 목록 조회
         */
}
