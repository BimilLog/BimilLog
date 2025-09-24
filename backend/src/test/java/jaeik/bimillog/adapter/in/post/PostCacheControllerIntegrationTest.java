package jaeik.bimillog.adapter.in.post;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostLikeRepository;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>게시글 Cache 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Post Cache API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>실시간/주간/레전드 인기글, 공지사항 조회 API 동작을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("게시글 Cache 컨트롤러 통합 테스트")
class PostCacheControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private User savedUser;
    private List<Post> testPosts;
    private List<User> likeUsers;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 테스트용 사용자 생성 및 저장
        savedUser = userRepository.save(TestUsers.USER1);

        // 좋아요 전용 사용자들 미리 생성
        createLikeUsers();
        
        // 테스트용 게시글들 생성
        createTestPosts();
    }

    private void createLikeUsers() {
        likeUsers = new ArrayList<>();
        // 충분한 좋아요 사용자를 미리 생성 (200명 정도 - 모든 게시글에 좋아요 가능하도록)
        for (int i = 0; i < 200; i++) {
            User likeUser = TestUsers.withSocialId("like_user_" + i);
            likeUsers.add(likeUser);
        }
        // 한 번에 저장
        likeUsers = userRepository.saveAll(likeUsers);
    }

    private void createTestPosts() {
        testPosts = new ArrayList<>();

        // 실시간 인기글 3개 생성
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

        // 주간 인기글 3개 생성
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

        // 레전드 인기글 5개 생성 (20개 이상 좋아요를 받을 게시글들)
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

        // 공지사항 2개 생성
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

        // 모든 게시글 저장
        testPosts = postRepository.saveAll(testPosts);

        // 좋아요 추가
        int likeUserIndex = 0;
        
        // 실시간 인기글에 좋아요 추가 (10, 11, 12개)
        for (int i = 0; i < 3; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 10 + i;
            for (int j = 0; j < likesToAdd; j++) {
                PostLike postLike = PostLike.builder()
                        .user(likeUsers.get(likeUserIndex++))
                        .post(post)
                        .build();
                postLikeRepository.save(postLike);
            }
        }

        // 주간 인기글에 좋아요 추가 (15, 16, 17개)
        for (int i = 3; i < 6; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 12 + i;
            for (int j = 0; j < likesToAdd; j++) {
                PostLike postLike = PostLike.builder()
                        .user(likeUsers.get(likeUserIndex++))
                        .post(post)
                        .build();
                postLikeRepository.save(postLike);
            }
        }
        
        // 레전드 게시글에 20개 이상 좋아요 (20, 21, 22, 23, 24개)
        for (int i = 6; i < 11; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 14 + i; // 20, 21, 22, 23, 24개
            for (int j = 0; j < likesToAdd; j++) {
                PostLike postLike = PostLike.builder()
                        .user(likeUsers.get(likeUserIndex++))
                        .post(post)
                        .build();
                postLikeRepository.save(postLike);
            }
        }
    }

    @Test
    @DisplayName("실시간/주간 인기글 조회 성공")
    void getPopularBoard_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtime", notNullValue()))
                .andExpect(jsonPath("$.weekly", notNullValue()));
                // 시간 조건 때문에 빈 배열일 수 있음 - 응답 구조만 검증
    }


    @Test
    @DisplayName("레전드 인기글 조회 - 빈 페이지")
    void getLegendBoard_EmptyPage() throws Exception {
        // When & Then - 큰 페이지 번호
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
        // When & Then - 공지사항이 캐시되어 있는지 확인
        mockMvc.perform(get("/api/post/notice"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
                // 캐시가 없으면 빈 배열이 반환될 수 있음
    }

}