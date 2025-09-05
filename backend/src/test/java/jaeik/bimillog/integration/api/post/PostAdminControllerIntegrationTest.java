package jaeik.bimillog.integration.api.post;

import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.post.PostJpaRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>게시글 Admin 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Post Admin API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>관리자 권한이 필요한 공지 설정/해제 API 동작을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("게시글 Admin 컨트롤러 통합 테스트")
class PostAdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private CustomUserDetails adminUser;
    private CustomUserDetails normalUser;
    private User savedAdminUser;
    private User savedNormalUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 관리자 사용자 생성 및 저장
        User adminUserEntity = User.builder()
                .socialId("admin123")
                .userName("관리자")
                .thumbnailImage("http://admin-profile.jpg")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.ADMIN)
                .setting(Setting.createSetting())
                .build();
        
        savedAdminUser = userRepository.save(adminUserEntity);
        
        // 관리자 CustomUserDetails 생성
        UserDTO adminDTO = UserDTO.builder()
                .userId(savedAdminUser.getId())
                .userName(savedAdminUser.getUserName())
                .role(savedAdminUser.getRole())
                .socialId(savedAdminUser.getSocialId())
                .provider(savedAdminUser.getProvider())
                .settingId(savedAdminUser.getSetting().getId())
                .socialNickname(savedAdminUser.getSocialNickname())
                .thumbnailImage(savedAdminUser.getThumbnailImage())
                .tokenId(1L)
                .fcmTokenId(null)
                .build();
        
        adminUser = new CustomUserDetails(adminDTO);

        // 일반 사용자 생성 및 저장
        User normalUserEntity = User.builder()
                .socialId("user123")
                .userName("일반사용자")
                .thumbnailImage("http://user-profile.jpg")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(Setting.createSetting())
                .build();
        
        savedNormalUser = userRepository.save(normalUserEntity);
        
        // 일반 사용자 CustomUserDetails 생성
        UserDTO userDTO = UserDTO.builder()
                .userId(savedNormalUser.getId())
                .userName(savedNormalUser.getUserName())
                .role(savedNormalUser.getRole())
                .socialId(savedNormalUser.getSocialId())
                .provider(savedNormalUser.getProvider())
                .settingId(savedNormalUser.getSetting().getId())
                .socialNickname(savedNormalUser.getSocialNickname())
                .thumbnailImage(savedNormalUser.getThumbnailImage())
                .tokenId(2L)
                .fcmTokenId(null)
                .build();
        
        normalUser = new CustomUserDetails(userDTO);

        // 테스트용 게시글 생성
        createTestPost();
    }

    private void createTestPost() {
        testPost = Post.builder()
                .user(savedNormalUser)
                .title("테스트 게시글")
                .content("테스트 게시글 내용입니다.")
                .password(123456)
                .views(0)
                .isNotice(false)
                .build();

        testPost = postJpaRepository.save(testPost);
    }

    @Test
    @DisplayName("게시글 공지 설정 성공 - 관리자 권한")
    void setPostAsNotice_Success_WithAdminRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // 실제 DB 확인
        Post updatedPost = postJpaRepository.findById(testPost.getId()).orElseThrow();
        assert updatedPost.isNotice() == true;
    }

    @Test
    @DisplayName("게시글 공지 설정 실패 - 일반 사용자 권한 없음")
    void setPostAsNotice_Fail_WithUserRole() throws Exception {
        // When & Then - 403 Forbidden 예상
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(normalUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // DB 확인 - 공지로 설정되지 않음
        Post unchangedPost = postJpaRepository.findById(testPost.getId()).orElseThrow();
        assert unchangedPost.isNotice() == false;
    }

    @Test
    @DisplayName("게시글 공지 설정 실패 - 인증되지 않은 사용자")
    void setPostAsNotice_Fail_Unauthorized() throws Exception {
        // When & Then - 401 Unauthorized 예상
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 공지 설정 실패 - 존재하지 않는 게시글")
    void setPostAsNotice_Fail_PostNotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        // When & Then - 404 Not Found 예상
        mockMvc.perform(post("/api/post/{postId}/notice", nonExistentPostId)
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 공지 해제 성공 - 관리자 권한")
    void unsetPostAsNotice_Success_WithAdminRole() throws Exception {
        // Given - 먼저 공지로 설정
        testPost.setAsNotice();
        postJpaRepository.save(testPost);

        // When & Then
        mockMvc.perform(delete("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // 실제 DB 확인
        Post updatedPost = postJpaRepository.findById(testPost.getId()).orElseThrow();
        assert updatedPost.isNotice() == false;
    }

    @Test
    @DisplayName("게시글 공지 해제 실패 - 일반 사용자 권한 없음")
    void unsetPostAsNotice_Fail_WithUserRole() throws Exception {
        // Given - 먼저 공지로 설정
        testPost.setAsNotice();
        postJpaRepository.save(testPost);

        // When & Then - 403 Forbidden 예상
        mockMvc.perform(delete("/api/post/{postId}/notice", testPost.getId())
                        .with(user(normalUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // DB 확인 - 여전히 공지 상태
        Post unchangedPost = postJpaRepository.findById(testPost.getId()).orElseThrow();
        assert unchangedPost.isNotice() == true;
    }

    @Test
    @DisplayName("게시글 공지 해제 실패 - 인증되지 않은 사용자")
    void unsetPostAsNotice_Fail_Unauthorized() throws Exception {
        // When & Then - 401 Unauthorized 예상
        mockMvc.perform(delete("/api/post/{postId}/notice", testPost.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 공지 해제 실패 - 존재하지 않는 게시글")
    void unsetPostAsNotice_Fail_PostNotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        // When & Then - 404 Not Found 예상
        mockMvc.perform(delete("/api/post/{postId}/notice", nonExistentPostId)
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미 공지인 게시글을 다시 공지 설정 - 멱등성 확인")
    void setPostAsNotice_Idempotent() throws Exception {
        // Given - 먼저 공지로 설정
        testPost.setAsNotice();
        postJpaRepository.save(testPost);

        // When & Then - 다시 공지 설정해도 성공
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // 여전히 공지 상태
        Post updatedPost = postJpaRepository.findById(testPost.getId()).orElseThrow();
        assert updatedPost.isNotice() == true;
    }

    @Test
    @DisplayName("이미 공지가 아닌 게시글을 공지 해제 - 멱등성 확인")
    void unsetPostAsNotice_Idempotent() throws Exception {
        // Given - 공지가 아닌 상태 확인
        assert testPost.isNotice() == false;

        // When & Then - 공지 해제해도 성공
        mockMvc.perform(delete("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // 여전히 공지가 아닌 상태
        Post updatedPost = postJpaRepository.findById(testPost.getId()).orElseThrow();
        assert updatedPost.isNotice() == false;
    }
}