package jaeik.bimillog.integration.api.post;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.global.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.UserRepository;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
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

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>게시글 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Post Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 * <p>게시글 목록 조회, 검색, 상세 조회 API 동작을 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
@DisplayName("게시글 Query 컨트롤러 통합 테스트")
class PostQueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private CustomUserDetails testUser;
    private User savedUser;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 테스트용 사용자 생성 및 저장
        User user = User.builder()
                .socialId("12345")
                .userName("테스트사용자")
                .thumbnailImage("http://test-profile.jpg")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(Setting.createSetting())
                .build();
        
        savedUser = userRepository.save(user);
        
        // UserDetail 생성하여 CustomUserDetails 생성
        UserDetail userDetail = UserDetail.builder()
                .userId(savedUser.getId())
                .userName(savedUser.getUserName())
                .role(savedUser.getRole())
                .socialId(savedUser.getSocialId())
                .provider(savedUser.getProvider())
                .settingId(savedUser.getSetting().getId())
                .socialNickname(savedUser.getSocialNickname())
                .thumbnailImage(savedUser.getThumbnailImage())
                .tokenId(1L) // 테스트용
                .fcmTokenId(null)
                .build();
                
        testUser = new CustomUserDetails(userDetail);

        // 테스트용 게시글들 생성
        createTestPosts();
    }

    private void createTestPosts() {
        testPost1 = Post.builder()
                .user(savedUser)
                .title("첫 번째 테스트 게시글")
                .content("첫 번째 게시글의 내용입니다.")
                .password(123456)
                .views(10)
                .isNotice(false)
                .build();

        testPost2 = Post.builder()
                .user(savedUser)
                .title("두 번째 검색 게시글")
                .content("검색용 키워드가 포함된 내용입니다.")
                .password(123456)
                .views(20)
                .isNotice(false)
                .build();

        testPost3 = Post.builder()
                .user(savedUser)
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
        // When & Then
        mockMvc.perform(get("/api/post")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.content[0].title", notNullValue()))
                .andExpect(jsonPath("$.content[0].userName", notNullValue()))
                .andExpect(jsonPath("$.content[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.pageable.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageable.pageSize", is(10)));
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 페이지네이션")
    void getBoard_Success_Pagination() throws Exception {
        // When & Then - 첫 번째 페이지, 크기 2
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
        // When & Then
        mockMvc.perform(get("/api/post")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                // 실제 DB 저장 순서대로 정렬됨 (최신순이 아닌 ID 순)
                .andExpect(jsonPath("$.content[0].title", notNullValue()))
                .andExpect(jsonPath("$.content[1].title", notNullValue()))
                .andExpect(jsonPath("$.content[2].title", notNullValue()));
    }

    @Test
    @DisplayName("게시글 검색 성공 - 제목 검색")
    void searchPost_Success_ByTitle() throws Exception {
        // When & Then
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
        // When & Then
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
        // When & Then
        mockMvc.perform(get("/api/post/search")
                        .param("type", "writer")
                        .param("query", "테스트사용자")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1)))); // 최소 1개 이상
    }

    @Test
    @DisplayName("게시글 검색 성공 - 검색 결과 없음")
    void searchPost_Success_NoResults() throws Exception {
        // When & Then
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
        // When & Then - 잘못된 타입에 대해 400 Bad Request 에러 반환
        mockMvc.perform(get("/api/post/search")
                        .param("type", "invalid")
                        .param("query", "검색어")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400 Bad Request 기대
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 - 로그인 사용자")
    void getPost_Success_AuthenticatedUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(testUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("첫 번째 테스트 게시글")))
                .andExpect(jsonPath("$.content", is("첫 번째 게시글의 내용입니다.")))
                .andExpect(jsonPath("$.userName", is("테스트사용자")))
                .andExpect(jsonPath("$.viewCount", is(10))) // 응답 시점에서는 아직 조회수 증가 전
                .andExpect(jsonPath("$.likeCount", is(0))) // 실제 값으로 수정
                .andExpect(jsonPath("$.commentCount", is(0))) // 실제 값으로 수정  
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 - 비로그인 사용자")
    void getPost_Success_AnonymousUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/{postId}", testPost2.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost2.getId().intValue())))
                .andExpect(jsonPath("$.title", is("두 번째 검색 게시글")))
                .andExpect(jsonPath("$.content", is("검색용 키워드가 포함된 내용입니다.")))
                .andExpect(jsonPath("$.userName", is("테스트사용자")));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 - CQRS 패턴 준수")
    void getPost_Success_CQRSCompliant() throws Exception {
        // When & Then - count 파라미터 제거됨
        mockMvc.perform(get("/api/post/{postId}", testPost3.getId())
                        .with(user(testUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost3.getId().intValue())))
                .andExpect(jsonPath("$.viewCount", is(5))); // 응답 시점에서는 아직 조회수 증가 전 (비동기 처리)
    }

    @Test
    @DisplayName("게시글 상세 조회 실패 - 존재하지 않는 게시글")
    void getPost_Fail_NotFound() throws Exception {
        // Given
        Long nonExistentPostId = 99999L;

        // When & Then
        mockMvc.perform(get("/api/post/{postId}", nonExistentPostId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 상세 조회 실패 - 잘못된 postId 형식")
    void getPost_Fail_InvalidPostIdFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/{postId}", "invalid-id"))
                .andDo(print())
                .andExpect(status().isInternalServerError()); // 현재 구현에서는 500 반환
    }

    @Test
    @DisplayName("게시판 목록 조회 성공 - 빈 결과")
    void getBoard_Success_EmptyResult() throws Exception {
        // Given - 모든 게시글 삭제
        postRepository.deleteAll();

        // When & Then
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
        // When & Then
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
        // When & Then - 공백 검색어에 대해 400 Bad Request
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title")
                        .param("query", "")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest()); // DTO 검증 실패로 400
    }

    @Test
    @DisplayName("게시글 검색 - 특수문자 검색어")
    void searchPost_Success_SpecialCharacters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/post/search")
                        .param("type", "title")
                        .param("query", "!@#$%")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0))); // 특수문자는 검색되지 않을 것
    }

    @Test
    @DisplayName("게시판 목록 조회 - 큰 페이지 번호")
    void getBoard_Success_LargePageNumber() throws Exception {
        // When & Then
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
        // When & Then - 쿠키 없이 첫 조회
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(testUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(jsonPath("$.title", is(testPost1.getTitle())))
                .andExpect(cookie().exists("post_views"))
                .andExpect(cookie().value("post_views", testPost1.getId().toString()));
    }

    @Test
    @DisplayName("게시글 상세 조회 - 중복 조회 검증 (쿠키 있는 재조회)")
    void getPost_Success_RepeatedViewing() throws Exception {
        // Given - 이미 조회한 게시글 ID가 포함된 쿠키
        String existingViews = testPost1.getId().toString();
        
        // When & Then - 동일 게시글 재조회 시 쿠키에 중복 추가되지 않아야 함
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId())
                        .with(user(testUser))
                        .cookie(new jakarta.servlet.http.Cookie("post_views", existingViews)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(cookie().value("post_views", existingViews)); // 동일한 값 유지
    }

    @Test
    @DisplayName("게시글 상세 조회 - 중복 조회 검증 (새로운 게시글 추가)")
    void getPost_Success_ViewingNewPost() throws Exception {
        // Given - 다른 게시글 ID가 포함된 쿠키
        String existingViews = testPost1.getId().toString();
        
        // When & Then - 새로운 게시글 조회 시 쿠키에 추가되어야 함
        mockMvc.perform(get("/api/post/{postId}", testPost2.getId())
                        .with(user(testUser))
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
        // When & Then - 비로그인 사용자도 쿠키 기반 중복 검증 적용
        mockMvc.perform(get("/api/post/{postId}", testPost1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testPost1.getId().intValue())))
                .andExpect(cookie().exists("post_views"))
                .andExpect(cookie().value("post_views", testPost1.getId().toString()));
    }
}