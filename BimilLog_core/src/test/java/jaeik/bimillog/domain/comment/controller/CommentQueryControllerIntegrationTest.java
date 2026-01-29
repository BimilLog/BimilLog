package jaeik.bimillog.domain.comment.controller;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.repository.CommentRepository;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.builder.CommentTestDataBuilder;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
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
 * <p>BFF 방식으로 인기 댓글 + 일반 댓글을 통합 조회</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginAdapterConfig.class})
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
        testPost = PostTestDataBuilder.createPost(testMember, "테스트 게시글", "테스트 게시글 내용입니다.");
        postRepository.save(testPost);

        for (int i = 1; i <= 5; i++) {
            Comment comment = CommentTestDataBuilder.createComment(
                    testPost, testMember, "테스트 댓글 " + i);
            commentRepository.save(comment);
        }
    }

    @Test
    @DisplayName("댓글 통합 조회 - 인기댓글 + 일반댓글 (BFF)")
    void getComments_WithAndWithoutData_IntegrationTest() throws Exception {
        // When & Then - 케이스 1: 댓글 있음 (testPost, setUpChild()에서 5개 생성)
        mockMvc.perform(get("/api/comment/{postId}", testPost.getId())
                .param("page", "0")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularCommentList").isArray())
                .andExpect(jsonPath("$.commentInfoPage.content").isArray())
                .andExpect(jsonPath("$.commentInfoPage.content.length()").value(5))
                .andExpect(jsonPath("$.commentInfoPage.totalElements").value(5));

        // Given - 케이스 2: 댓글이 없는 게시글
        Post emptyPost = PostTestDataBuilder.createPost(testMember, "빈 게시글", "댓글이 없는 게시글입니다.");
        postRepository.save(emptyPost);

        // When & Then - 케이스 2: 댓글 없음
        mockMvc.perform(get("/api/comment/{postId}", emptyPost.getId())
                .param("page", "0")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popularCommentList").isEmpty())
                .andExpect(jsonPath("$.commentInfoPage.content").isEmpty())
                .andExpect(jsonPath("$.commentInfoPage.totalElements").value(0));
    }
}
