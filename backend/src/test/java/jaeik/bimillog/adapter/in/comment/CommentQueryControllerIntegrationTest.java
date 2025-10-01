package jaeik.bimillog.adapter.in.comment;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.infrastructure.adapter.out.comment.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.out.post.PostRepository;
import jaeik.bimillog.testutil.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginPortConfig.class})
@DisplayName("댓글 Query 컨트롤러 통합 테스트")
@Tag("integration")
class CommentQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    private Post testPost;
    
    @Override
    protected void setUpChild() {
        // testUser는 BaseIntegrationTest에서 이미 생성됨
        testPost = PostTestDataBuilder.createPost(testMember, "테스트 게시글", "테스트 게시글 내용입니다.");
        postRepository.save(testPost);
        
        // 테스트용 댓글들 생성
        for (int i = 1; i <= 5; i++) {
            Comment comment = CommentTestDataBuilder.createComment(
                    testPost, testMember, "테스트 댓글 " + i);
            commentRepository.save(comment);
        }
    }
    
    @Test
    @DisplayName("댓글 조회 통합 테스트 - 첫 번째 페이지")
    void getComments_FirstPage_IntegrationTest() throws Exception {
        // Given - testUserDetails는 BaseIntegrationTest에서 이미 생성됨
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}", testPost.getId())
                .param("page", "0")
                .with(user(testUserDetails)))
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
        Post emptyPost = PostTestDataBuilder.createPost(testMember, "빈 게시글", "댓글이 없는 게시글입니다.");
        postRepository.save(emptyPost);
        

        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}", emptyPost.getId())
                .param("page", "0")
                .with(user(testUserDetails)))
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
        // Given - testUserDetails는 BaseIntegrationTest에서 이미 생성됨
        
        // When & Then
        mockMvc.perform(get("/api/comment/{postId}/popular", testPost.getId())
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        // 추천 로직은 별도 CommentLike 엔티티로 관리되므로 실제 추천이 없으면 빈 배열 반환이 정상
    }
}
