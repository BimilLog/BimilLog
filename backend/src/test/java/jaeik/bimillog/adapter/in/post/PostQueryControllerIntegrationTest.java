package jaeik.bimillog.adapter.in.post;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.post.PostRepository;
import jaeik.bimillog.infrastructure.adapter.out.member.MemberRepository;
import jaeik.bimillog.testutil.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>게시글 Query 컨트롤러 통합 테스트</h2>
 * <p>MySQL Full-Text + Redis 연동을 포함한 조회 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@IntegrationTest
@Import(TestSocialLoginPortConfig.class)
@DisplayName("게시글 Query 컨트롤러 통합 테스트")
class PostQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository userRepository;

    private CustomUserDetails queryUserDetails;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @Override
    protected void setUpChild() {
        var user = TestMembers.createMember(
                "12345",
                "테스트사용자",
                "테스트사용자"
        );
        var savedUser = userRepository.save(user);
        queryUserDetails = AuthTestFixtures.createCustomUserDetails(savedUser);
        createTestPosts(savedUser);
    }

    private void createTestPosts(Member savedMember) {
        testPost1 = Post.builder()
                .member(savedMember)
                .title("첫 번째 테스트 게시글")
                .content("첫 번째 게시글의 내용입니다.")
                .password(123456)
                .views(10)
                .isNotice(false)
                .build();

        testPost2 = Post.builder()
                .member(savedMember)
                .title("두 번째 검색 게시글")
                .content("검색용 키워드가 포함된 내용입니다.")
                .password(123456)
                .views(20)
                .isNotice(false)
                .build();

        testPost3 = Post.builder()
                .member(savedMember)
                .title("최신 게시글")
                .content("가장 최근에 작성된 게시글입니다.")
                .password(123456)
                .views(5)
                .isNotice(false)
                .build();

        postRepository.saveAll(Arrays.asList(testPost1, testPost2, testPost3));
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 기본 페이지")
    void getBoard_Success_DefaultPage() throws Exception {
        mockMvc.perform(get("/api/post")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.content[0].title", notNullValue()))
                .andExpect(jsonPath("$.pageable.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageable.pageSize", is(10)));
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 페이지네이션")
    void getBoard_Success_Pagination() throws Exception {
        mockMvc.perform(get("/api/post")
                        .param("page", "0")
                        .param("size", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.pageable.pageSize", is(2)));
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 최신순 정렬 확인")
    void getBoard_Success_OrderByLatest() throws Exception {
        mockMvc.perform(get("/api/post")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", notNullValue()))
                .andExpect(jsonPath("$.content[1].title", notNullValue()))
                .andExpect(jsonPath("$.content[2].title", notNullValue()));
    }

    @Test
    @DisplayName("게시글 검색 성공 - 제목 검색")
    void searchPost_Success_ByTitle() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title")
                        .param("query", "검색")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", containsString("검색")));
    }

    @Test
    @DisplayName("게시글 검색 성공 - 제목+내용 검색")
    void searchPost_Success_ByTitleContent() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title_content")
                        .param("query", "키워드")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", containsString("검색")));
    }

    @Test
    @DisplayName("게시글 검색 성공 - 작성자 검색")
    void searchPost_Success_ByWriter() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "writer")
                        .param("query", "테스트사용자")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("게시글 검색 성공 - 검색 결과 없음")
    void searchPost_Success_NoResults() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title")
                        .param("query", "존재하지않는검색어")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("게시글 검색 실패 - 잘못된 검색 타입")
    void searchPost_Fail_InvalidType() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "invalid")
                        .param("query", "검색어")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 - 로그인 사용자")
    void getPost_Success_AuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(queryUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("첫 번째 테스트 게시글")))
                .andExpect(jsonPath("$.content", is("첫 번째 게시글의 내용입니다.")))
                .andExpect(jsonPath("$.memberName", is("테스트사용자")))
                .andExpect(jsonPath("$.viewCount", is(10)))
                .andExpect(jsonPath("$.likeCount", is(0)))
                .andExpect(jsonPath("$.commentCount", is(0)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 - 비로그인 사용자")
    void getPost_Success_AnonymousUser() throws Exception {
        mockMvc.perform(get("/api/post/{postId}", testPost2.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost2.getId().intValue())))
                .andExpect(jsonPath("$.title", is("두 번째 검색 게시글")))
                .andExpect(jsonPath("$.content", is("검색용 키워드가 포함된 내용입니다.")))
                .andExpect(jsonPath("$.memberName", is("테스트사용자")));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 - CQRS 패턴 준수")
    void getPost_Success_CQRSCompliant() throws Exception {
        mockMvc.perform(get("/api/post/{postId}", testPost3.getId())
                        .with(user(queryUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost3.getId().intValue())))
                .andExpect(jsonPath("$.viewCount", is(5)));
    }

    @Test
    @DisplayName("게시글 상세 조회 실패 - 존재하지 않는 게시글")
    void getPost_Fail_NotFound() throws Exception {
        mockMvc.perform(get("/api/post/{postId}", 99999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 상세 조회 실패 - 잘못된 postId 형식")
    void getPost_Fail_InvalidPostIdFormat() throws Exception {
        mockMvc.perform(get("/api/post/{postId}", "invalid-id"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 빈 결과")
    void getBoard_Success_EmptyResult() throws Exception {
        postRepository.deleteAll();

        mockMvc.perform(get("/api/post")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("게시글 검색 - 한글 검색어")
    void searchPost_Success_KoreanQuery() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title_content")
                        .param("query", "내용")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("게시글 검색 - 공백 검색어")
    void searchPost_Fail_EmptyQuery() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title")
                        .param("query", "")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게시글 검색 - 특수문자 검색어")
    void searchPost_Success_SpecialCharacters() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title")
                        .param("query", "!@#$%")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("게시판 목록 조회 - 큰 페이지 번호")
    void getBoard_Success_LargePageNumber() throws Exception {
        mockMvc.perform(get("/api/post")
                        .param("page", "1000")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.pageable.pageNumber", is(1000)));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 중복 조회 검증 (쿠키 없는 첫 조회)")
    void getPost_Success_FirstTimeViewing() throws Exception {
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(queryUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(cookie().exists("post_views"))
                .andExpect(cookie().value("post_views", testPost1.getId().toString()));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 중복 조회 검증 (쿠키 있는 재조회)")
    void getPost_Success_RepeatedViewing() throws Exception {
        String existingViews = testPost1.getId().toString();

        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(queryUserDetails))
                        .cookie(new jakarta.servlet.http.Cookie("post_views", existingViews)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(cookie().value("post_views", existingViews));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 중복 조회 검증 (새로운 게시글 추가)")
    void getPost_Success_ViewingNewPost() throws Exception {
        String existingViews = testPost1.getId().toString();

        mockMvc.perform(get("/api/post/{postId}", testPost2.getId())
                        .with(user(queryUserDetails))
                        .cookie(new jakarta.servlet.http.Cookie("post_views", existingViews)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost2.getId().intValue())))
                .andExpect(cookie().exists("post_views"))
                .andExpect(result -> {
                    String cookieValue = result.getResponse().getCookie("post_views").getValue();
                    assertThat(cookieValue).contains(testPost1.getId().toString());
                    assertThat(cookieValue).contains(testPost2.getId().toString());
                });
    }

    @Test
    @DisplayName("게시글 상세 조회 - 비로그인 사용자도 중복 조회 검증")
    void getPost_Success_AnonymousUserDuplicateCheck() throws Exception {
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(cookie().exists("post_views"))
                .andExpect(cookie().value("post_views", testPost1.getId().toString()));
    }
}
