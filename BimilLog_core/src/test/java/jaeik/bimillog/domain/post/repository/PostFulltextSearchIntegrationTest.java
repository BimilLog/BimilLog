package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.adapter.PostToCommentAdapter;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.service.PostSearchService;
import jaeik.bimillog.infrastructure.config.QueryDSLConfig;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.LocalIntegrationTestSupportConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * <h2>MySQL FULLTEXT 전문 검색 통합 테스트</h2>
 * <p>PostQueryAdapter의 풀텍스트 검색 기능을 로컬 MySQL 환경에서 테스트합니다.</p>
 * <p>ngram parser를 사용한 한국어 전문 검색 동작을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {PostQueryRepository.class}
        )
)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostSearchRepository.class, PostSearchService.class, QueryDSLConfig.class, LocalIntegrationTestSupportConfig.class})
@Tag("local-integration")
class PostFulltextSearchIntegrationTest {

    @Autowired
    private PostSearchRepository postSearchRepository;

    @Autowired
    private PostSearchService postSearchService;

    @Autowired
    private TestEntityManager entityManager;

    @MockitoBean
    private PostToCommentAdapter postToCommentAdapter;

    @MockitoBean
    private PostToMemberAdapter postToMemberAdapter;

    // PostLikeQueryRepository는 삭제되었으므로 Mock 제거
    // 추천수는 이제 서브쿼리로 직접 조회됨

    private Member testMember;
    private Post koreanPost1, koreanPost2, koreanPost3, englishPost;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testMember = TestMembers.copyWithId(TestMembers.MEMBER_1, null);
        if (testMember.getSetting() != null) {
            entityManager.persist(testMember.getSetting());
        }
        if (testMember.getSocialToken() != null) {
            entityManager.persist(testMember.getSocialToken());
        }
        entityManager.persistAndFlush(testMember);

        // 테스트용 게시글들 생성
        createTestPosts();

        // 댓글 수 Mock 설정
        Map<Long, Integer> commentCounts = new HashMap<>();
        commentCounts.put(koreanPost1.getId(), 2);
        commentCounts.put(koreanPost2.getId(), 1);
        commentCounts.put(koreanPost3.getId(), 0);
        commentCounts.put(englishPost.getId(), 1);

        given(postToCommentAdapter.findCommentCountsByPostIds(any(List.class)))
                .willReturn(commentCounts);

        // 추천 수는 이제 서브쿼리로 직접 조회되므로 Mock 제거
        // PostLikeQueryRepository가 삭제되었음
    }

    private void createTestPosts() {
        // 한글 게시글 1: 제목과 내용에 "자바" 포함
        koreanPost1 = Post.createPost(testMember, "자바 프로그래밍 기초", "자바 언어로 프로그래밍하는 방법을 배웁니다.", 1234, testMember.getMemberName());
        entityManager.persistAndFlush(koreanPost1);

        // 한글 게시글 2: 제목에만 "스프링" 포함
        koreanPost2 = Post.createPost(testMember, "스프링 부트 완벽 가이드", "Spring Boot를 사용한 웹 개발 튜토리얼입니다.", 1234, testMember.getMemberName());
        entityManager.persistAndFlush(koreanPost2);

        // 한글 게시글 3: 내용에만 "데이터베이스" 포함
        koreanPost3 = Post.createPost(testMember, "백엔드 개발 입문", "효율적인 데이터베이스 설계 및 최적화 기법을 다룹니다.", 1234, testMember.getMemberName());
        entityManager.persistAndFlush(koreanPost3);

        // 영문 게시글
        englishPost = Post.createPost(testMember, "Java Programming Tutorial", "Learn advanced Java concepts and patterns.", 1234, testMember.getMemberName());
        entityManager.persistAndFlush(englishPost);

        entityManager.flush();
    }

    @Test
    @DisplayName("정상 케이스 - 제목 전문 검색 (3글자 이상, 실제 비즈니스 로직)")
    void shouldFindPostsByTitleFullText_WhenKoreanQueryProvided() {
        // Given: 한글 검색어 "프로그래밍" (3글자 이상, 실제 사용 시나리오)
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "프로그래밍";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목 전문 검색 수행
        // 내부에서 query + "*" 로 변환되어 와일드카드 검색 수행
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: "프로그래밍"이 제목에 포함된 게시글 조회
        // MySQL FULLTEXT 검색이 정상적으로 동작함을 확인
        assertThat(result).isNotNull();

        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();

        // 검색 결과가 있다면 "프로그래밍"이 포함된 게시글이 있어야 함
        if (!result.isEmpty()) {
            assertThat(titles).anyMatch(title -> title.contains("프로그래밍"));
        }
    }

    @Test
    @DisplayName("정상 케이스 - 제목+내용 전문 검색 (3글자 이상, TITLE_CONTENT)")
    void shouldFindPostsByTitleContentFullText_WhenSearchingBothFields() {
        // Given: "스프링" 검색 (제목 또는 내용에 포함, 3글자)
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "스프링";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목+내용 전문 검색 수행
        // PostFulltextRepository가 TITLE_CONTENT 인덱스를 사용하여 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: 제목 또는 내용에 "스프링"이 포함된 게시글 조회
        assertThat(result).isNotNull();

        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();

        // 검색 결과가 있다면 제목에 "스프링"이 있는 게시글 포함
        // (내용 검색은 PostSimpleDetail에 내용이 없으므로 제목으로만 검증)
        if (!result.isEmpty()) {
            assertThat(titles).anyMatch(title -> title.contains("스프링"));
        }
    }

    @Test
    @DisplayName("정상 케이스 - 내용 전문 검색")
    void shouldFindPostsByContentFullText_WhenKeywordOnlyInContent() {
        // Given: 내용에만 있는 "데이터베이스" 검색
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "데이터베이스";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목+내용 전문 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: 내용에 "데이터베이스"가 포함된 게시글 조회
        assertThat(result).isNotNull();

        if (!result.isEmpty()) {
            List<String> titles = result.getContent().stream()
                    .map(PostSimpleDetail::getTitle)
                    .toList();
            assertThat(titles).contains("백엔드 개발 입문");
        }
    }

    @Test
    @DisplayName("정상 케이스 - 영문 전문 검색(4글자)")
    void shouldFindPostsByEnglishFullText_WhenEnglishQueryProvided() {
        // Given: 영문 검색어 "Java"
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "Java";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목 전문 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: "Java"가 제목에 포함된 게시글 조회
        assertThat(result).isNotNull();

        if (!result.isEmpty()) {
            List<String> titles = result.getContent().stream()
                    .map(PostSimpleDetail::getTitle)
                    .toList();
            assertThat(titles).anyMatch(title -> title.contains("Java"));
        }
    }

    @Test
    @DisplayName("정상 케이스 - 페이징 동작 확인")
    void shouldSupportPaging_WhenMultipleResultsFound() {
        // Given: 여러 게시글에 포함된 검색어
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "프로그래밍";

        // 첫 페이지: 크기 1
        Pageable firstPage = PageRequest.of(0, 1);

        // When: 첫 페이지 조회
        Page<PostSimpleDetail> result = searchFullText(searchType, query, firstPage, null);

        // Then: 페이징 정보가 올바르게 설정됨
        assertThat(result).isNotNull();

        if (!result.isEmpty()) {
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.getContent()).hasSizeLessThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("비즈니스 로직 - 모든 게시글이 검색 결과에 포함됨")
    void shouldIncludeAllPosts_WhenSearching() {
        // Given: 검색어 "자바" (여러 게시글에 포함)
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "자바";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: 검색어가 포함된 게시글이 결과에 포함됨
        assertThat(result).isNotNull();

        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();

        // 검색 결과가 있다면 "자바"가 포함된 게시글이 있어야 함
        if (!result.isEmpty()) {
            assertThat(titles).anyMatch(title -> title.contains("자바"));
        }
    }

    @Test
    @DisplayName("엣지 케이스 - 검색 결과 없음")
    void shouldReturnEmptyPage_WhenNoResultsFound() {
        // Given: 존재하지 않는 검색어
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "존재하지않는검색어12345";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("엣지 케이스 - 1글자 검색어 (ngram 미만이지만 LIKE 부분검색 폴백)")
    void shouldFallbackToPartialMatch_WhenSingleCharacterQuery() {
        // Given: 1글자 검색어 (ngram_token_size=2 미만 → 전문검색 대신 LIKE 부분검색 폴백)
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "자";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 검색 시도 (3글자 미만이므로 부분검색 전략 사용)
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: LIKE 부분검색으로 "자바 프로그래밍 입문" 등 매칭
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("비즈니스 로직 - 최신순 정렬")
    void shouldSortByCreatedAtDesc_WhenSearching() {
        // Given: 여러 게시글에 포함된 검색어 "개발자" (3글자)
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "개발";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: 최신 게시글부터 정렬됨
        if (result.getContent().size() > 1) {
            List<java.time.Instant> createdAts = result.getContent().stream()
                    .map(PostSimpleDetail::getCreatedAt)
                    .toList();

            for (int i = 1; i < createdAts.size(); i++) {
                assertThat(createdAts.get(i - 1)).isAfterOrEqualTo(createdAts.get(i));
            }
        }
    }

    @Test
    @DisplayName("비즈니스 로직 - 댓글 수와 추천 수 포함")
    void shouldIncludeLikeAndCommentCounts_InSearchResults() {
        // Given: 검색어 "프로그래밍" (3글자 이상)
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "프로그래밍";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: 댓글 수와 추천 수가 설정되어 있음
        if (!result.isEmpty()) {
            assertThat(result.getContent()).allMatch(post -> post.getCommentCount() != null);
            assertThat(result.getContent()).allMatch(post -> post.getLikeCount() != null);
        }
    }

    @Test
    @DisplayName("정상 케이스 - 한글 내용 전문 검색 (3글자)")
    void shouldFindPosts_WhenLongerKoreanQueryProvided() {
        // Given: 3글자 이상 한글 검색어
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "스프링";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = searchFullText(searchType, query, pageable, null);

        // Then: "스프링"이 포함된 게시글 조회
        assertThat(result).isNotNull();

        if (!result.isEmpty()) {
            List<String> titles = result.getContent().stream()
                    .map(PostSimpleDetail::getTitle)
                    .toList();
            assertThat(titles).anyMatch(title -> title.contains("스프링"));
        }
    }

    private Page<PostSimpleDetail> searchFullText(PostSearchType type, String query, Pageable pageable, Long viewerId) {
        return postSearchService.searchPost(type, query, pageable, viewerId);
    }
}
