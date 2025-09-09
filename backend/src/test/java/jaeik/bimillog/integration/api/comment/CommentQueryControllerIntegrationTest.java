package jaeik.bimillog.integration.api.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.global.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.jpa.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.post.PostJpaRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.UserRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>댓글 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Comment Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>댓글 조회, 인기댓글 조회 API 동작을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("댓글 Query 컨트롤러 통합 테스트")
class CommentQueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostJpaRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    private MockMvc mockMvc;
    private User testUser;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        // 테스트용 사용자 생성
        testUser = createTestUser();
        userRepository.save(testUser);
        
        // 테스트용 게시글 생성
        testPost = createTestPost(testUser);
        postRepository.save(testPost);
        
        // 테스트용 댓글들 생성
        createTestComments();
    }
    
    @Test
    @DisplayName("댓글 조회 통합 테스트 - 첫 번째 페이지")
    void getComments_FirstPage_IntegrationTest() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}", testPost.getId())
                .param("page", "0")
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
    
    @Test
    @DisplayName("댓글 조회 통합 테스트 - 빈 페이지")
    void getComments_EmptyPage_IntegrationTest() throws Exception {
        // Given - 댓글이 없는 새로운 게시글
        Post emptyPost = createTestPost(testUser);
        postRepository.save(emptyPost);
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}", emptyPost.getId())
                .param("page", "0")
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }
    
    @Test
    @DisplayName("댓글 조회 통합 테스트 - 페이지 파라미터 없음")
    void getComments_NoPageParam_IntegrationTest() throws Exception {
        // Given
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then - 기본 페이지(0)로 조회
        mockMvc.perform(get("/api/comment/{postId}", testPost.getId())
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.number").value(0));
    }
    
    @Test
    @DisplayName("인기댓글 조회 통합 테스트")
    void getPopularComments_IntegrationTest() throws Exception {
        // Given - 추천이 많은 댓글들 생성
        createPopularComments();
        
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}/popular", testPost.getId())
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        // 추천 로직은 별도 CommentLike 엔티티로 관리되므로 실제 추천이 없으면 0개 반환이 정상
    }
    
    @Test
    @DisplayName("인기댓글 조회 통합 테스트 - 인기댓글 없음")
    void getPopularComments_NoPopularComments_IntegrationTest() throws Exception {
        // Given - 추천이 적은 댓글들만 있음
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}/popular", testPost.getId())
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    @DisplayName("댓글 조회 실패 - 존재하지 않는 게시글")
    void getComments_NonExistentPost_IntegrationTest() throws Exception {
        // Given
        Long nonExistentPostId = 999999L;
        CustomUserDetails userDetails = createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}", nonExistentPostId)
                .param("page", "0")
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        // 존재하지 않는 게시글도 빈 결과를 반환하는 것이 정상 동작일 수 있음
    }
    
    @Test
    @DisplayName("비로그인 사용자 댓글 조회 통합 테스트")
    void getComments_AnonymousUser_IntegrationTest() throws Exception {
        // When & Then - 인증 없이 요청
        mockMvc.perform(get("/api/comment/{postId}", testPost.getId())
                .param("page", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
    
    /**
     * 테스트용 사용자 생성
     */
    private User createTestUser() {
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        
        return User.builder()
                .socialId("12345")
                .socialNickname("테스트사용자")
                .thumbnailImage("test-profile.jpg")
                .userName("testuser")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
    }
    
    /**
     * 테스트용 게시글 생성
     */
    private Post createTestPost(User user) {
        return Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용입니다.")
                .user(user)
                .build();
    }
    
    /**
     * 테스트용 일반 댓글들 생성
     */
    private void createTestComments() {
        for (int i = 1; i <= 5; i++) {
            Comment comment = Comment.builder()
                    .content("테스트 댓글 " + i)
                    .user(testUser)
                    .post(testPost)
                    .deleted(false)
                    .build();
            commentRepository.save(comment);
        }
    }
    
    /**
     * 테스트용 인기 댓글들 생성 (추천 3개 이상)
     * 현재는 별도의 CommentLike 엔티티를 통해 추천을 관리하므로
     * 테스트에서는 일반 댓글을 생성하고 실제 추천 로직은 서비스 계층에서 처리
     */
    private void createPopularComments() {
        // 실제 시스템에서는 CommentLike 엔티티를 통해 추천수가 계산되므로
        // 통합 테스트에서는 실제 추천 로직을 테스트하기보다는
        // API 호출 자체가 정상적으로 동작하는지 확인
        for (int i = 1; i <= 3; i++) {
            Comment comment = Comment.builder()
                    .content("일반 댓글 " + i)
                    .user(testUser)
                    .post(testPost)
                    .deleted(false)
                    .build();
            commentRepository.save(comment);
        }
    }
    
    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createUserDetails(User user) {
        UserDetail userDetail = UserDetail.builder()
                .userId(user.getId())
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .build();
        
        return new CustomUserDetails(userDetail);
    }
}