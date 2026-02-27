package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.repository.CommentRepository;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.builder.CommentTestDataBuilder;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>MyPageController 통합 테스트</h2>
 * <p>마이페이지 API 엔드포인트를 검증하는 통합 테스트</p>
 * <p>H2 데이터베이스를 사용하여 실제 HTTP 요청/응답 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("MyPageController 통합 테스트")
@ActiveProfiles("h2test")
@Import(H2TestConfiguration.class)
@Tag("springboot-h2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyPageControllerTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    private Post testPost;
    private Comment testComment;

    @Override
    protected void setUpChild() {
        // 테스트 게시글 생성 (testMember가 작성)
        testPost = PostTestDataBuilder.createPost(testMember, "테스트 게시글", "테스트 내용");
        testPost = postRepository.save(testPost);

        // 테스트 댓글 생성 (testMember가 작성)
        testComment = CommentTestDataBuilder.createComment(testPost, testMember, "테스트 댓글");
        testComment = commentRepository.save(testComment);
    }

    // ==================== GET /api/mypage/ ====================

    @Test
    @DisplayName("마이페이지 정보 조회 성공 - 200 OK 및 사용자 활동 정보 반환")
    void shouldGetMyPageInfo_Successfully() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/mypage")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberActivityComment").exists())
                .andExpect(jsonPath("$.memberActivityPost").exists())
                .andExpect(jsonPath("$.memberActivityComment.writeComments").exists())
                .andExpect(jsonPath("$.memberActivityPost.writePosts").exists());
    }

    @Test
    @DisplayName("마이페이지 정보 조회 성공 - 작성한 게시글 포함 확인")
    void shouldGetMyPageInfo_WithPosts() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/mypage")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberActivityPost.writePosts.content").isArray())
                .andExpect(jsonPath("$.memberActivityPost.writePosts.content[0].title").value(testPost.getTitle()));
    }

    @Test
    @DisplayName("마이페이지 정보 조회 성공 - 작성한 댓글 포함 확인")
    void shouldGetMyPageInfo_WithComments() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/mypage")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberActivityComment.writeComments.content").isArray())
                .andExpect(jsonPath("$.memberActivityComment.writeComments.content[0].content").value(testComment.getContent()));
    }

    @Test
    @DisplayName("마이페이지 정보 조회 실패 - 인증되지 않은 사용자 (403 Forbidden)")
    void shouldReturn403_WhenUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/mypage")
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("마이페이지 정보 조회 성공 - 페이징 처리 확인")
    void shouldGetMyPageInfo_WithPagination() throws Exception {
        // Given - 추가 게시글 생성
        for (int i = 0; i < 5; i++) {
            Post post = PostTestDataBuilder.createPost(testMember, "테스트 게시글 " + i, "테스트 내용 " + i);
            postRepository.save(post);
        }

        // When & Then - 첫 페이지 조회 (size=3)
        mockMvc.perform(get("/api/mypage")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberActivityPost.writePosts.totalElements").value(6))  // 기존 1개 + 새로 생성한 5개
                .andExpect(jsonPath("$.memberActivityPost.writePosts.size").value(3))
                .andExpect(jsonPath("$.memberActivityPost.writePosts.number").value(0));
    }

    @Test
    @DisplayName("마이페이지 정보 조회 성공 - 작성 활동이 없는 사용자")
    void shouldGetMyPageInfo_WithNoActivity() throws Exception {
        // When & Then - otherMember는 작성 활동이 없음
        mockMvc.perform(get("/api/mypage")
                        .with(user(otherUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberActivityComment.writeComments.content").isEmpty())
                .andExpect(jsonPath("$.memberActivityPost.writePosts.content").isEmpty());
    }
}
