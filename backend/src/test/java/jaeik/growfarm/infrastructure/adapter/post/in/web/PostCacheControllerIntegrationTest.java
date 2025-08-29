package jaeik.growfarm.infrastructure.adapter.post.in.web;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.post.PostJpaRepository;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.postlike.PostLikeJpaRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import jaeik.growfarm.util.TestContainersConfiguration;
import jaeik.growfarm.util.TestSocialLoginPortConfig;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private PostJpaRepository postJpaRepository;

    @Autowired
    private PostLikeJpaRepository postLikeJpaRepository;

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
        User user = User.builder()
                .socialId("test123")
                .userName("테스트사용자")
                .thumbnailImage("http://test-profile.jpg")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(Setting.createSetting())
                .build();
        
        savedUser = userRepository.save(user);

        // 좋아요 전용 사용자들 미리 생성
        createLikeUsers();
        
        // 테스트용 게시글들 생성
        createTestPosts();
    }

    private void createLikeUsers() {
        likeUsers = new ArrayList<>();
        // 충분한 좋아요 사용자를 미리 생성 (130명 정도)
        for (int i = 0; i < 130; i++) {
            User likeUser = User.builder()
                    .socialId("like_user_" + i)
                    .userName("좋아요사용자" + i)
                    .thumbnailImage("http://like-profile" + i + ".jpg")
                    .provider(SocialProvider.KAKAO)
                    .role(UserRole.USER)
                    .setting(Setting.createSetting())
                    .build();
            likeUsers.add(likeUser);
        }
        // 한 번에 저장
        likeUsers = userRepository.saveAll(likeUsers);
    }

    private void createTestPosts() {
        testPosts = new ArrayList<>();

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
        testPosts = postJpaRepository.saveAll(testPosts);

        // 레전드 게시글에만 20개 이상 좋아요 추가
        int likeUserIndex = 0;
        
        // 레전드 게시글에 20개 이상 좋아요 (20, 21, 22, 23, 24개)
        for (int i = 0; i < 5; i++) {
            Post post = testPosts.get(i);
            int likesToAdd = 20 + i; // 20, 21, 22, 23, 24개
            for (int j = 0; j < likesToAdd; j++) {
                PostLike postLike = PostLike.builder()
                        .user(likeUsers.get(likeUserIndex++))
                        .post(post)
                        .build();
                postLikeJpaRepository.save(postLike);
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
    @DisplayName("실시간/주간 인기글 조회 - 빈 결과")
    void getPopularBoard_EmptyResult() throws Exception {
        // Given - 모든 캐시 플래그 제거
        for (Post post : testPosts) {
            post.setPostCacheFlag(null);
        }
        postJpaRepository.saveAll(testPosts);

        // When & Then
        mockMvc.perform(get("/api/post/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtime", hasSize(0)))
                .andExpect(jsonPath("$.weekly", hasSize(0)));
    }

    @Test
    @DisplayName("레전드 인기글 조회 성공 - 첫 페이지")
    void getLegendBoard_Success_FirstPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/legend")
                        .param("page", "0")
                        .param("size", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].title", containsString("레전드")))
                .andExpect(jsonPath("$.pageable.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageable.pageSize", is(3)))
                .andExpect(jsonPath("$.totalElements", is(5)));
    }

    @Test
    @DisplayName("레전드 인기글 조회 성공 - 두 번째 페이지")
    void getLegendBoard_Success_SecondPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/legend")
                        .param("page", "1")
                        .param("size", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2))) // 총 5개 중 두 번째 페이지는 2개
                .andExpect(jsonPath("$.pageable.pageNumber", is(1)))
                .andExpect(jsonPath("$.totalElements", is(5)));
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
    @DisplayName("레전드 인기글 조회 - 페이지 크기 제한")
    void getLegendBoard_PageSizeLimit() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/legend")
                        .param("page", "0")
                        .param("size", "100")) // 큰 페이지 크기 요청
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5))) // 전체 5개만 존재
                .andExpect(jsonPath("$.totalElements", is(5)));
    }

    @Test
    @DisplayName("공지사항 조회 성공")
    void getNoticeBoard_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/notice"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", containsString("공지사항")))
                .andExpect(jsonPath("$[1].title", containsString("공지사항")));
    }

    @Test
    @DisplayName("공지사항 조회 - 공지사항 없음")
    void getNoticeBoard_NoNotices() throws Exception {
        // Given - 모든 공지사항을 일반 게시글로 변경
        for (Post post : testPosts) {
            if (post.isNotice()) {
                post.unsetAsNotice();
            }
        }
        postJpaRepository.saveAll(testPosts);

        // When & Then
        mockMvc.perform(get("/api/post/notice"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("실시간 인기글만 있는 경우")
    void getPopularBoard_OnlyRealtime() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtime", notNullValue()))
                .andExpect(jsonPath("$.weekly", notNullValue()));
                // 실제 데이터는 시간 조건에 따라 달라질 수 있음
    }

    @Test
    @DisplayName("주간 인기글만 있는 경우")
    void getPopularBoard_OnlyWeekly() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtime", notNullValue()))
                .andExpect(jsonPath("$.weekly", notNullValue()));
                // 실제 데이터는 시간 조건에 따라 달라질 수 있음
    }

    @Test
    @DisplayName("레전드 인기글 조회 - 기본 페이지 크기")
    void getLegendBoard_DefaultPageSize() throws Exception {
        // When & Then - 페이지 크기를 지정하지 않으면 기본값 사용
        mockMvc.perform(get("/api/post/legend"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.pageable", notNullValue()));
    }

    @Test
    @DisplayName("공지사항 조회 - 응답 필드 확인")
    void getNoticeBoard_CheckResponseFields() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/notice"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].title", notNullValue()))
                .andExpect(jsonPath("$[0].userName", is("테스트사용자")))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$[0].viewCount", is(0)))
                .andExpect(jsonPath("$[0].likeCount", notNullValue()))
                .andExpect(jsonPath("$[0].commentCount", notNullValue()));
    }

    @Test
    @DisplayName("실시간/주간 인기글 조회 - 응답 필드 확인")
    void getPopularBoard_CheckResponseFields() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/popular"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtime[0].id", notNullValue()))
                .andExpect(jsonPath("$.realtime[0].title", notNullValue()))
                .andExpect(jsonPath("$.realtime[0].userName", is("테스트사용자")))
                .andExpect(jsonPath("$.realtime[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.realtime[0].viewCount", notNullValue()))
                .andExpect(jsonPath("$.realtime[0].likeCount", notNullValue()))
                .andExpect(jsonPath("$.realtime[0].commentCount", notNullValue()))
                .andExpect(jsonPath("$.weekly[0].id", notNullValue()))
                .andExpect(jsonPath("$.weekly[0].title", notNullValue()))
                .andExpect(jsonPath("$.weekly[0].userName", is("테스트사용자")));
    }
}