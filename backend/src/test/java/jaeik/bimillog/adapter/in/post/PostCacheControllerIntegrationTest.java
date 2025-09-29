package jaeik.bimillog.adapter.in.post;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.infrastructure.adapter.out.post.PostLikeRepository;
import jaeik.bimillog.infrastructure.adapter.out.post.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>게시글 Cache 컨트롤러 통합 테스트</h2>
 * <p>TestContainers 환경(MySQL + Redis)에서 캐시 기반 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@IntegrationTest
@Import(TestSocialLoginPortConfig.class)
@DisplayName("게시글 Cache 컨트롤러 통합 테스트")
class PostCacheControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    private User savedUser;
    private List<User> likeUsers;

    @Override
    protected void setUpChild() {
        savedUser = testUser;
        likeUsers = createLikeUsers();
        createTestPosts();
    }

    private List<User> createLikeUsers() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            users.add(TestUsers.withSocialId("like_user_" + i));
        }
        return userRepository.saveAll(users);
    }

    private void createTestPosts() {
        List<Post> testPosts = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Post realtimePost = Post.builder()
                    .user(savedUser)
                    .title("실시간 인기글 " + i)
                    .content("실시간 인기글 내용 " + i)
                    .password(123456)
                    .views(100 * i)
                    .isNotice(false)
                    .build();
            realtimePost.updatePostCacheFlag(PostCacheFlag.REALTIME);
            testPosts.add(realtimePost);
        }

        for (int i = 1; i <= 3; i++) {
            Post weeklyPost = Post.builder()
                    .user(savedUser)
                    .title("주간 인기글 " + i)
                    .content("주간 인기글 내용 " + i)
                    .password(123456)
                    .views(200 * i)
                    .isNotice(false)
                    .build();
            weeklyPost.updatePostCacheFlag(PostCacheFlag.WEEKLY);
            testPosts.add(weeklyPost);
        }

        for (int i = 1; i <= 5; i++) {
            Post legendPost = Post.builder()
                    .user(savedUser)
                    .title("레전드 인기글 " + i)
                    .content("레전드 인기글 내용 " + i)
                    .password(123456)
                    .views(500 * i)
                    .isNotice(false)
                    .build();
            legendPost.updatePostCacheFlag(PostCacheFlag.LEGEND);
            testPosts.add(legendPost);
        }

        for (int i = 1; i <= 2; i++) {
            Post noticePost = Post.builder()
                    .user(savedUser)
                    .title("공지사항 " + i)
                    .content("공지사항 내용 " + i)
                    .password(123456)
                    .views(0)
                    .isNotice(true)
                    .build();
            testPosts.add(noticePost);
        }

        testPosts = postRepository.saveAll(testPosts);

        int likeUserIndex = 0;

        for (int i = 0; i < 3; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 10 + i;
            for (int j = 0; j < likesToAdd; j++) {
                postLikeRepository.save(PostLike.builder()
                        .user(likeUsers.get(likeUserIndex++))
                        .post(post)
                        .build());
            }
        }

        for (int i = 3; i < 6; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 12 + i;
            for (int j = 0; j < likesToAdd; j++) {
                postLikeRepository.save(PostLike.builder()
                        .user(likeUsers.get(likeUserIndex++))
                        .post(post)
                        .build());
            }
        }

        for (int i = 6; i < 11; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 14 + i;
            for (int j = 0; j < likesToAdd; j++) {
                postLikeRepository.save(PostLike.builder()
                        .user(likeUsers.get(likeUserIndex++))
                        .post(post)
                        .build());
            }
        }
    }

    @Test
    @DisplayName("실시간/주간 인기글 조회 성공")
    void getPopularBoard_Success() throws Exception {
        mockMvc.perform(get("/api/post/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtime", notNullValue()))
                .andExpect(jsonPath("$.weekly", notNullValue()));
    }

    @Test
    @DisplayName("레전드 인기글 조회 - 빈 페이지")
    void getLegendBoard_EmptyPage() throws Exception {
        mockMvc.perform(get("/api/post/legend")
                        .param("page", "100")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.pageable.pageNumber", is(100)));
    }

    @Test
    @DisplayName("공지사항 조회 성공")
    void getNoticeBoard_Success() throws Exception {
        mockMvc.perform(get("/api/post/notice"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }
}
