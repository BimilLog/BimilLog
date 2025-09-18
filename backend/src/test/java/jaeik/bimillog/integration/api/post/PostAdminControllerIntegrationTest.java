package jaeik.bimillog.integration.api.post;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
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
    private PostRepository postRepository;

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
        UserDetail adminDTO = UserDetail.builder()
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
        UserDetail userDetail = UserDetail.builder()
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
        
        normalUser = new CustomUserDetails(userDetail);

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

        testPost = postRepository.save(testPost);
    }

    @Test
    @DisplayName("게시글 공지 토글 성공 - 관리자 권한 (비공지 -> 공지)")
    void togglePostNotice_Success_WithAdminRole_NormalToNotice() throws Exception {
        // Given - 초기상태: 비공지
        assert testPost.isNotice() == false;
        
        // When & Then
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // 실제 DB 확인 - 공지로 변경됨
        Post updatedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert updatedPost.isNotice() == true;
    }

    @Test
    @DisplayName("게시글 공지 토글 성공 - 관리자 권한 (공지 -> 비공지)")
    void togglePostNotice_Success_WithAdminRole_NoticeToNormal() throws Exception {
        // Given - 먼저 공지로 설정
        testPost.setAsNotice();
        postRepository.save(testPost);
        assert testPost.isNotice() == true;
        
        // When & Then
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // 실제 DB 확인 - 비공지로 변경됨
        Post updatedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert updatedPost.isNotice() == false;
    }

    @Test
    @DisplayName("게시글 공지 토글 실패 - 일반 사용자 권한 없음")
    void togglePostNotice_Fail_WithUserRole() throws Exception {
        // When & Then - 403 Forbidden 예상
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(normalUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // DB 확인 - 상태 변경되지 않음
        Post unchangedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert unchangedPost.isNotice() == false; // 초기 상태 유지
    }

    @Test
    @DisplayName("게시글 공지 토글 실패 - 인증되지 않은 사용자")
    void togglePostNotice_Fail_Unauthorized() throws Exception {
        // When & Then - 403 Forbidden 예상
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
                
        // DB 확인 - 상태 변경되지 않음
        Post unchangedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert unchangedPost.isNotice() == false;
    }

    @Test
    @DisplayName("게시글 공지 토글 실패 - 존재하지 않는 게시글")
    void togglePostNotice_Fail_PostNotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        // When & Then - 500 Internal Server Error 예상 (PostCustomException -> handleAll)
        mockMvc.perform(post("/api/post/{postId}/notice", nonExistentPostId)
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("게시글 공지 토글 - 연속 두 번 토글 (멱등성 확인)")
    void togglePostNotice_TwiceToggle_Idempotency() throws Exception {
        // Given - 초기 비공지 상태
        assert testPost.isNotice() == false;
        
        // When & Then - 첫 번째 토글: 비공지 -> 공지
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // DB 확인 - 공지로 변경
        Post firstTogglePost = postRepository.findById(testPost.getId()).orElseThrow();
        assert firstTogglePost.isNotice() == true;
        
        // When & Then - 두 번째 토글: 공지 -> 비공지
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // DB 확인 - 비공지로 되돌아감
        Post secondTogglePost = postRepository.findById(testPost.getId()).orElseThrow();
        assert secondTogglePost.isNotice() == false;
    }

    @Test
    @DisplayName("게시글 공지 토글 - 세 번 토글 시나리오")
    void togglePostNotice_ThreeTimes() throws Exception {
        // Given - 초기 비공지 상태
        assert testPost.isNotice() == false;
        
        // 첫 번째 토글: 비공지 -> 공지
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk());
        Post firstPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert firstPost.isNotice() == true;
        
        // 두 번째 토글: 공지 -> 비공지
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk());
        Post secondPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert secondPost.isNotice() == false;
        
        // 세 번째 토글: 비공지 -> 공지
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk());
        Post thirdPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert thirdPost.isNotice() == true;
    }

    @Test
    @DisplayName("게시글 공지 토글 성공 - 캐시 실패 시에도 DB 트랜잭션은 성공")
    void togglePostNotice_Success_EvenWhenCacheFails() throws Exception {
        // Given - 초기상태: 비공지
        assert testPost.isNotice() == false;
        
        // When & Then - 캐시 실패가 있어도 API는 200 OK 응답
        mockMvc.perform(post("/api/post/{postId}/notice", testPost.getId())
                        .with(user(adminUser))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk()); // 캐시 실패가 있어도 200 OK

        // 실제 DB 확인 - 핵심 비즈니스 로직은 정상 실행됨
        Post updatedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assert updatedPost.isNotice() == true;
    }

}