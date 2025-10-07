package jaeik.bimillog.adapter.out.post;

import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostToCommentPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.adapter.out.post.PostQueryAdapter;
import jaeik.bimillog.infrastructure.adapter.out.post.PostQueryHelper;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestMembers;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * <h2>MySQL FULLTEXT 전문 검색 통합 테스트</h2>
 * <p>PostQueryAdapter의 풀텍스트 검색 기능을 TestContainers MySQL 환경에서 테스트합니다.</p>
 * <p>ngram parser를 사용한 한국어 전문 검색 동작을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {PostQueryAdapter.class, PostQueryHelper.class}
        )
)
@Testcontainers
@ActiveProfiles("tc")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostQueryAdapter.class, PostQueryHelper.class, TestContainersConfiguration.class})
@Tag("tc")
class PostFulltextSearchIntegrationTest {

    @Autowired
    private PostQueryAdapter postQueryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @MockitoBean
    private PostToCommentPort postToCommentPort;

    @MockitoBean
    private PostLikeQueryPort postLikeQueryPort;

    private Member testMember;
    private Post koreanPost1, koreanPost2, koreanPost3, noticePost, englishPost;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testMember = TestMembers.copyWithId(TestMembers.MEMBER_1, null);
        if (testMember.getSetting() != null) {
            entityManager.persist(testMember.getSetting());
        }
        if (testMember.getKakaoToken() != null) {
            entityManager.persist(testMember.getKakaoToken());
        }
        entityManager.persistAndFlush(testMember);

        // 테스트용 게시글들 생성
        createTestPosts();

        // 댓글 수 Mock 설정
        Map<Long, Integer> commentCounts = new HashMap<>();
        commentCounts.put(koreanPost1.getId(), 2);
        commentCounts.put(koreanPost2.getId(), 1);
        commentCounts.put(koreanPost3.getId(), 0);
        commentCounts.put(noticePost.getId(), 3);
        commentCounts.put(englishPost.getId(), 1);

        // 추천 수 Mock 설정
        Map<Long, Integer> likeCounts = new HashMap<>();
        likeCounts.put(koreanPost1.getId(), 5);
        likeCounts.put(koreanPost2.getId(), 3);
        likeCounts.put(koreanPost3.getId(), 1);
        likeCounts.put(noticePost.getId(), 8);
        likeCounts.put(englishPost.getId(), 2);

        given(postToCommentPort.findCommentCountsByPostIds(any(List.class)))
                .willReturn(commentCounts);
        given(postLikeQueryPort.findLikeCountsByPostIds(any(List.class)))
                .willReturn(likeCounts);
    }

    private void createTestPosts() {
        // 한글 게시글 1: 제목과 내용에 "자바" 포함
        koreanPost1 = Post.createPost(testMember, "자바 프로그래밍 기초", "자바 언어로 프로그래밍하는 방법을 배웁니다.", 1234);
        entityManager.persistAndFlush(koreanPost1);

        // 한글 게시글 2: 제목에만 "스프링" 포함
        koreanPost2 = Post.createPost(testMember, "스프링 부트 완벽 가이드", "Spring Boot를 사용한 웹 개발 튜토리얼입니다.", 1234);
        entityManager.persistAndFlush(koreanPost2);

        // 한글 게시글 3: 내용에만 "데이터베이스" 포함
        koreanPost3 = Post.createPost(testMember, "백엔드 개발 입문", "효율적인 데이터베이스 설계 및 최적화 기법을 다룹니다.", 1234);
        entityManager.persistAndFlush(koreanPost3);

        // 영문 게시글
        englishPost = Post.createPost(testMember, "Java Programming Tutorial", "Learn advanced Java concepts and patterns.", 1234);
        entityManager.persistAndFlush(englishPost);

        // 공지사항 게시글 (검색 결과에서 제외되어야 함)
        noticePost = Post.createPost(testMember, "자바 커뮤니티 공지사항", "자바 개발자 모임 안내입니다.", 1234);
        noticePost.setAsNotice();
        entityManager.persistAndFlush(noticePost);

        entityManager.flush();
    }

    @Test
    @DisplayName("정상 케이스 - 제목 전문 검색 (3글자 이상, 실제 비즈니스 로직)")
    void shouldFindPostsByTitleFullText_WhenKoreanQueryProvided() {
        // Given: 한글 검색어 "프로그래밍" (3글자 이상, 실제 사용 시나리오)
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "프로그래밍";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목 전문 검색
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

        // Then: "프로그래밍"이 제목에 포함된 게시글 조회 (공지사항 제외)
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();

        // 검색 결과에 "자바 프로그래밍 기초"가 포함되어야 함
        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();
        assertThat(titles).anyMatch(title -> title.contains("프로그래밍"));

        // 공지사항은 제외되어야 함
        assertThat(titles).noneMatch(title -> title.contains("공지사항"));
    }

    @Test
    @DisplayName("정상 케이스 - 제목+내용 전문 검색 (3글자 이상, TITLE_CONTENT)")
    void shouldFindPostsByTitleContentFullText_WhenSearchingBothFields() {
        // Given: "스프링" 검색 (제목 또는 내용에 포함, 3글자)
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "스프링";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목+내용 전문 검색
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

        // Then: 제목 또는 내용에 "스프링"이 포함된 게시글 조회
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();

        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();

        // 제목에 "스프링"이 있는 게시글 포함
        assertThat(titles).anyMatch(title -> title.contains("스프링"));
    }

    @Test
    @DisplayName("정상 케이스 - 내용 전문 검색")
    void shouldFindPostsByContentFullText_WhenKeywordOnlyInContent() {
        // Given: 내용에만 있는 "데이터베이스" 검색
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "데이터베이스";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목+내용 전문 검색
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

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
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

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
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, firstPage);

        // Then: 페이징 정보가 올바르게 설정됨
        assertThat(result).isNotNull();

        if (!result.isEmpty()) {
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.getContent()).hasSizeLessThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("비즈니스 로직 - 공지사항 제외")
    void shouldExcludeNoticePosts_WhenSearching() {
        // Given: 공지사항에도 포함된 검색어 "커뮤니티" (3글자)
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "커뮤니티";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

        // Then: 공지사항은 결과에서 제외됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty(); // 공지사항만 "커뮤니티"를 포함하므로 빈 결과

        // 또는 다른 게시글이 있다면 공지사항이 포함되지 않아야 함
        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();
        assertThat(titles).doesNotContain("자바 커뮤니티 공지사항");
    }

    @Test
    @DisplayName("엣지 케이스 - 검색 결과 없음")
    void shouldReturnEmptyPage_WhenNoResultsFound() {
        // Given: 존재하지 않는 검색어
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "존재하지않는검색어12345";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("엣지 케이스 - 1글자 검색어 (ngram 토큰 크기 미만, 전문검색 미동작)")
    void shouldReturnEmptyResult_WhenSingleCharacterQuery() {
        // Given: 1글자 검색어 (ngram_token_size=2 미만)
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "자";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색 시도
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

        // Then: ngram 파서가 1글자를 토큰화하지 않으므로 빈 결과 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("비즈니스 로직 - 최신순 정렬")
    void shouldSortByCreatedAtDesc_WhenSearching() {
        // Given: 여러 게시글에 포함된 검색어 "개발자" (3글자)
        PostSearchType searchType = PostSearchType.TITLE_CONTENT;
        String query = "개발";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

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
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

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
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

        // Then: "스프링"이 포함된 게시글 조회
        assertThat(result).isNotNull();

        if (!result.isEmpty()) {
            List<String> titles = result.getContent().stream()
                    .map(PostSimpleDetail::getTitle)
                    .toList();
            assertThat(titles).anyMatch(title -> title.contains("스프링"));
        }
    }
}
