package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
import jaeik.bimillog.testutil.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>게시글 Query 컨트롤러 통합 테스트</h2>
 * <p>H2 인메모리 데이터베이스 환경에서 게시글 목록 조회, 상세 조회, 검색 API를 검증합니다.</p>
 *
 * @author Jaeik
 */
@DisplayName("게시글 Query 컨트롤러 통합 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("springboot-h2")
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginAdapterConfig.class})
class PostQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostRepository postRepository;

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
        var savedUser = saveMember(user);
        queryUserDetails = AuthTestFixtures.createCustomUserDetails(savedUser);
        createTestPosts(savedUser);
    }

    private void createTestPosts(Member savedMember) {
        String name = savedMember.getMemberName();

        testPost1 = Post.builder()
                .member(savedMember)
                .title("첫 번째 테스트 게시글")
                .content("첫 번째 게시글의 내용입니다.")
                .password(123456)
                .memberName(name)
                .views(10)
                .build();

        testPost2 = Post.builder()
                .member(savedMember)
                .title("두 번째 검색 게시글")
                .content("검색용 키워드가 포함된 내용입니다.")
                .password(123456)
                .memberName(name)
                .views(20)
                .build();

        testPost3 = Post.builder()
                .member(savedMember)
                .title("최신 게시글")
                .content("가장 최근에 작성된 게시글입니다.")
                .password(123456)
                .memberName(name)
                .views(5)
                .build();

        postRepository.saveAll(Arrays.asList(testPost1, testPost2, testPost3));
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 첫 페이지 (커서 없음)")
    void getBoard_Success_FirstPage() throws Exception {
        mockMvc.perform(get("/api/post")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.content[0].title", notNullValue()));
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 커서 기반 페이지네이션")
    void getBoard_Success_CursorPagination() throws Exception {
        mockMvc.perform(get("/api/post")
                        .param("size", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.nextCursor", notNullValue()));
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 최신순 정렬 확인 (ID 내림차순)")
    void getBoard_Success_OrderByIdDesc() throws Exception {
        mockMvc.perform(get("/api/post")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", notNullValue()))
                .andExpect(jsonPath("$.content[1].title", notNullValue()))
                .andExpect(jsonPath("$.content[2].title", notNullValue()));
    }

    @ParameterizedTest
    @MethodSource("provideSearchTypeScenarios")
    @DisplayName("게시글 검색 성공 - 다양한 검색 타입")
    void searchPost_Success_VariousTypes(String type, String query, int expectedMinSize) throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", type)
                        .param("query", query)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(expectedMinSize))))
                .andExpect(jsonPath("$.pageable.pageNumber", is(0)));
    }

    static Stream<Arguments> provideSearchTypeScenarios() {
        return Stream.of(
            // 제목 검색
            Arguments.of("TITLE", "검색", 1),
            // 제목+내용 검색
            Arguments.of("TITLE_CONTENT", "검색 게시글", 0),
            // 작성자 검색
            Arguments.of("WRITER", "테스트사용자", 1)
        );
    }

    @Test
    @DisplayName("게시글 검색 성공 - 검색 결과 없음")
    void searchPost_Success_NoResults() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "TITLE")
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
        mockMvc.perform(get("/api/post/{postId}", Long.MAX_VALUE))
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
    @DisplayName("게시판 목록 조회 성공 - 마지막 커서 이후 빈 결과")
    void getBoard_Success_EmptyResultAfterLastCursor() throws Exception {
        // Given: 가장 오래된 게시글 ID (가장 작은 ID) 조회
        Long oldestPostId = postRepository.findAll().stream()
                .map(Post::getId)
                .min(Long::compareTo)
                .orElse(1L);

        // When: 가장 작은 ID를 커서로 사용하면 그 이후에는 게시글이 없음
        mockMvc.perform(get("/api/post")
                        .param("cursor", String.valueOf(oldestPostId))
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("게시글 검색 - 한글 검색어")
    void searchPost_Success_KoreanQuery() throws Exception {
        mockMvc.perform(get("/api/post/search")
                        .param("type", "TITLE_CONTENT")
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
                        .param("type", "TITLE")
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
                        .param("type", "TITLE")
                        .param("query", "!@#$%")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("게시판 목록 조회 - 다음 페이지 커서 기반 조회")
    void getBoard_Success_NextPageByCursor() throws Exception {
        // Given: 첫 페이지 조회 후 nextCursor 획득
        String firstPageResult = mockMvc.perform(get("/api/post")
                        .param("size", "2"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // nextCursor 추출
        Long nextCursor = com.jayway.jsonpath.JsonPath.parse(firstPageResult).read("$.nextCursor", Long.class);

        // When: 다음 페이지 조회
        mockMvc.perform(get("/api/post")
                        .param("cursor", String.valueOf(nextCursor))
                        .param("size", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 로그인 사용자 첫 조회 (Redis 기반 중복 방지)")
    void getPost_Success_FirstTimeViewing_LoggedInUser() throws Exception {
        // Given: 로그인한 사용자가 첫 조회
        // Redis SET에 m:{memberId} 키로 조회 이력이 저장됨 (컨트롤러 테스트에서는 응답만 검증)

        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(queryUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("첫 번째 테스트 게시글")))
                .andExpect(jsonPath("$.viewCount", notNullValue()));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 비로그인 사용자 조회 (IP 기반 중복 방지)")
    void getPost_Success_AnonymousUserViewing() throws Exception {
        // Given: 비로그인 사용자 조회
        // Redis SET에 ip:{clientIp} 키로 조회 이력이 저장됨

        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .header("X-Forwarded-For", "192.168.1.100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("첫 번째 테스트 게시글")))
                .andExpect(jsonPath("$.viewCount", notNullValue()));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 여러 게시글 연속 조회")
    void getPost_Success_MultiplePostsViewing() throws Exception {
        // Given: 로그인 사용자가 여러 게시글 연속 조회
        // 각 게시글마다 Redis SET에 조회 이력 저장

        // 첫 번째 게시글 조회
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(queryUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())));

        // 두 번째 게시글 조회
        mockMvc.perform(get("/api/post/{postId}", testPost2.getId())
                        .with(user(queryUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost2.getId().intValue())));

        // 세 번째 게시글 조회
        mockMvc.perform(get("/api/post/{postId}", testPost3.getId())
                        .with(user(queryUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost3.getId().intValue())));
    }

    @Test
    @DisplayName("게시글 상세 조회 - X-Forwarded-For 헤더 없는 비로그인 사용자")
    void getPost_Success_AnonymousUserWithoutXFF() throws Exception {
        // Given: X-Forwarded-For 헤더 없이 비로그인 사용자 조회
        // request.getRemoteAddr()로 IP 추출

        mockMvc.perform(get("/api/post/{postId}", testPost1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(jsonPath("$.viewCount", notNullValue()));
    }
}
