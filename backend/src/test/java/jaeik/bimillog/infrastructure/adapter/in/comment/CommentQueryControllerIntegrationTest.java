package jaeik.bimillog.infrastructure.adapter.in.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.testutil.*;
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
    private PostRepository postRepository;
    
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
        testUser = TestUsers.createUnique();
        userRepository.save(testUser);
        
        // 테스트용 게시글 생성
        testPost = TestFixtures.createPostWithUser(testUser);
        postRepository.save(testPost);
        
        // 테스트용 댓글들 생성
        createTestComments();
    }
    
    @Test
    @DisplayName("댓글 조회 통합 테스트 - 첫 번째 페이지")
    void getComments_FirstPage_IntegrationTest() throws Exception {
        // Given
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
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
        Post emptyPost = TestFixtures.createPostWithId(null, testUser, "빈 게시글", "댓글이 없는 게시글입니다.");
        postRepository.save(emptyPost);
        
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
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
    @DisplayName("인기댓글 조회 통합 테스트")
    void getPopularComments_IntegrationTest() throws Exception {
        // Given
        CustomUserDetails userDetails = CommentTestDataBuilder.createUserDetails(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}/popular", testPost.getId())
                .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        // 추천 로직은 별도 CommentLike 엔티티로 관리되므로 실제 추천이 없으면 빈 배열 반환이 정상
    }
    

    
    /**
     * 테스트용 일반 댓글들 생성
     */
    private void createTestComments() {
        for (int i = 1; i <= 5; i++) {
            Comment comment = CommentTestDataBuilder.createTestComment(
                    testUser, testPost, "테스트 댓글 " + i);
            commentRepository.save(comment);
        }
    }

}