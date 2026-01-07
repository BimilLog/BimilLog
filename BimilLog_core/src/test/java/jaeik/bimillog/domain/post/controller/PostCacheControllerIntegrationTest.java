package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.repository.PostLikeRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
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
@Import(TestSocialLoginAdapterConfig.class)
@DisplayName("게시글 Cache 컨트롤러 통합 테스트")
class PostCacheControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    private Member savedMember;
    private List<Member> likeMembers;

    @Override
    protected void setUpChild() {
        savedMember = testMember;
        likeMembers = createLikeUsers();
        createTestPosts();
    }

    private List<Member> createLikeUsers() {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            members.add(TestMembers.withSocialId("like_user_" + i));
        }
        return saveMembers(members);
    }

    private void createTestPosts() {
        List<Post> testPosts = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Post realtimePost = Post.builder()
                    .member(savedMember)
                    .title("실시간 인기글 " + i)
                    .content("실시간 인기글 내용 " + i)
                    .password(123456)
                    .views(100 * i)
                    .build();
            testPosts.add(realtimePost);
        }

        for (int i = 1; i <= 3; i++) {
            Post weeklyPost = Post.builder()
                    .member(savedMember)
                    .title("주간 인기글 " + i)
                    .content("주간 인기글 내용 " + i)
                    .password(123456)
                    .views(200 * i)
                    .build();
            testPosts.add(weeklyPost);
        }

        for (int i = 1; i <= 5; i++) {
            Post legendPost = Post.builder()
                    .member(savedMember)
                    .title("레전드 인기글 " + i)
                    .content("레전드 인기글 내용 " + i)
                    .password(123456)
                    .views(500 * i)
                    .build();
            testPosts.add(legendPost);
        }

        for (int i = 1; i <= 2; i++) {
            Post noticePost = Post.builder()
                    .member(savedMember)
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
                        .member(likeMembers.get(likeUserIndex++))
                        .post(post)
                        .build());
            }
        }

        for (int i = 3; i < 6; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 12 + i;
            for (int j = 0; j < likesToAdd; j++) {
                postLikeRepository.save(PostLike.builder()
                        .member(likeMembers.get(likeUserIndex++))
                        .post(post)
                        .build());
            }
        }

        for (int i = 6; i < 11; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 14 + i;
            for (int j = 0; j < likesToAdd; j++) {
                postLikeRepository.save(PostLike.builder()
                        .member(likeMembers.get(likeUserIndex++))
                        .post(post)
                        .build());
            }
        }
    }

    @Test
    @DisplayName("실시간 인기글 조회 성공")
    void getRealtimePopularPosts_Success() throws Exception {
        mockMvc.perform(get("/api/post/realtime"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", isA(List.class)));
    }

    @Test
    @DisplayName("주간 인기글 조회 성공")
    void getWeeklyPopularPosts_Success() throws Exception {
        mockMvc.perform(get("/api/post/weekly"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", isA(List.class)));
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
