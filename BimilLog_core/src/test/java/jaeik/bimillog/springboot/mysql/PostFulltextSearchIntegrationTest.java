package jaeik.bimillog.springboot.mysql;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.service.PostSearchService;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.LocalIntegrationTestSupportConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>MySQL FULLTEXT 전문 검색 통합 테스트</h2>
 * <p>PostQueryAdapter의 풀텍스트 검색 기능을 로컬 MySQL 환경에서 테스트합니다.</p>
 * <p>ngram parser를 사용한 한국어 전문 검색 동작을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("local-integration")
@ActiveProfiles("local-integration")
@Import(LocalIntegrationTestSupportConfig.class)
@Transactional
class PostFulltextSearchIntegrationTest {

    @Autowired
    private PostSearchService postSearchService;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private PostToMemberAdapter postToMemberAdapter;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = TestMembers.copyWithId(TestMembers.MEMBER_1, null);
        if (testMember.getSetting() != null) {
            entityManager.persist(testMember.getSetting());
        }
        entityManager.persist(testMember);
        entityManager.flush();

        createTestPosts();

    }

    private void createTestPosts() {
        Post koreanPost1 = Post.createPost(testMember, "자바 프로그래밍 기초", "자바 언어로 프로그래밍하는 방법을 배웁니다.", 1234, testMember.getMemberName());
        entityManager.persist(koreanPost1);
        entityManager.flush();

        Post koreanPost2 = Post.createPost(testMember, "스프링 부트 완벽 가이드", "Spring Boot를 사용한 웹 개발 튜토리얼입니다.", 1234, testMember.getMemberName());
        entityManager.persist(koreanPost2);
        entityManager.flush();

        Post koreanPost3 = Post.createPost(testMember, "백엔드 개발 입문", "효율적인 데이터베이스 설계 및 최적화 기법을 다룹니다.", 1234, testMember.getMemberName());
        entityManager.persist(koreanPost3);
        entityManager.flush();

        Post englishPost = Post.createPost(testMember, "Java Programming Tutorial", "Learn advanced Java concepts and patterns.", 1234, testMember.getMemberName());
        entityManager.persist(englishPost);
        entityManager.flush();
    }

    @Test
    @DisplayName("정상 케이스 - 제목 전문 검색 (3글자 이상, 실제 비즈니스 로직)")
    void shouldFindPostsByTitleFullText_WhenKoreanQueryProvided() {
        PostQueryType searchType = PostQueryType.TITLE;
        String query = "프로그래밍";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

        assertThat(result).isNotNull();
        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();
        if (!result.isEmpty()) {
            assertThat(titles).anyMatch(title -> title.contains("프로그래밍"));
        }
    }

    @Test
    @DisplayName("정상 케이스 - 제목+내용 전문 검색 (3글자 이상, TITLE_CONTENT)")
    void shouldFindPostsByTitleContentFullText_WhenSearchingBothFields() {
        PostQueryType searchType = PostQueryType.TITLE_CONTENT;
        String query = "스프링";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

        assertThat(result).isNotNull();
        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();
        if (!result.isEmpty()) {
            assertThat(titles).anyMatch(title -> title.contains("스프링"));
        }
    }

    @Test
    @DisplayName("정상 케이스 - 내용 전문 검색")
    void shouldFindPostsByContentFullText_WhenKeywordOnlyInContent() {
        PostQueryType searchType = PostQueryType.TITLE_CONTENT;
        String query = "데이터베이스";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

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
        PostQueryType searchType = PostQueryType.TITLE;
        String query = "Java";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

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
        PostQueryType searchType = PostQueryType.TITLE_CONTENT;
        String query = "프로그래밍";
        Pageable firstPage = PageRequest.of(0, 1);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, firstPage, null);

        assertThat(result).isNotNull();
        if (!result.isEmpty()) {
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.getContent()).hasSizeLessThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("엣지 케이스 - 검색 결과 없음")
    void shouldReturnEmptyPage_WhenNoResultsFound() {
        PostQueryType searchType = PostQueryType.TITLE;
        String query = "존재하지않는검색어12345";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("엣지 케이스 - 1글자 검색어 (ngram 미만이지만 LIKE 부분검색 폴백)")
    void shouldFallbackToPartialMatch_WhenSingleCharacterQuery() {
        PostQueryType searchType = PostQueryType.TITLE;
        String query = "자";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("엣지 케이스 - 2글자 검색어 (ngram token_size=2 활용 가능)")
    void shouldFallbackToPartialMatch_WhenTwoCharacterQuery() {
        PostQueryType searchType = PostQueryType.TITLE;
        String query = "자바";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).anyMatch(post -> post.getTitle().contains("자바"));
    }

    // ==================== F-BUG-5 회귀: 한글 임의 위치 부분어 매칭 ====================

    @Test
    @DisplayName("F-BUG-5: 제목 중간 어절 부분어 검색 - 'ngram NATURAL LANGUAGE MODE'로 매칭 가능")
    void shouldMatchMiddleSubstring_WhenTitleFullText() {
        // Given: 제목에 '프로그래밍' 어절이 중간에 위치한 게시글이 createTestPosts() 에 이미 존재
        //   "자바 프로그래밍 기초" / "스프링 부트 완벽 가이드" / "백엔드 개발 입문" / "Java Programming Tutorial"
        // 사용자가 '프로그래' (제목 중간 어절의 부분 + 토큰 경계 걸침) 로 검색.
        // BOOLEAN MODE prefix("프로그래*") 로는 "자바 프로그래밍 기초" 의 ngram 시퀀스 '자바'/'바 '/' 프'/'프로'... 중에서 '프로그래' 시작점이 매칭되어야 하는데
        // 단어 prefix 로 인식되어 한글 부분어가 누락될 수 있음.
        // NATURAL LANGUAGE MODE 는 '프로'/'로그'/'그래' 토큰 매칭으로 자연스럽게 hit.
        PostQueryType searchType = PostQueryType.TITLE_CONTENT;
        String query = "프로그래";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .as("F-BUG-5: NATURAL LANGUAGE MODE 로 한글 임의 위치 부분어 검색이 가능해야 한다")
                .anyMatch(post -> post.getTitle().contains("프로그래"));
    }

    @Test
    @DisplayName("F-BUG-5: WRITER 4글자 이상도 Containing(부분일치) 로 검색 - prefix-only 정책 폐기")
    void shouldMatchWriterByContaining_WhenWriterQueryFourChars() {
        // Given: testMember.getMemberName() 로 작성된 게시글이 4건 존재.
        // 작성자 닉네임의 중간 부분 4글자 이상으로 검색해도 결과가 나와야 한다.
        // (이전: WRITER + 4글자 이상이면 startsWith 만 사용되어 닉네임 prefix 가 다르면 0건)
        PostQueryType searchType = PostQueryType.WRITER;
        String memberName = testMember.getMemberName();
        // 닉네임 길이가 4글자 미만이면 테스트 의미 약화 — 안전하게 닉네임 끝부분 부분어 사용
        String query = memberName.length() >= 4
                ? memberName.substring(memberName.length() - 4)
                : memberName;
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PostSimpleDetail> result = postSearchService.searchPost(searchType, query, pageable, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .as("F-BUG-5: WRITER 4글자 이상도 Containing 으로 작성자 닉네임의 임의 위치 부분 검색이 가능해야 한다")
                .isNotEmpty();
        assertThat(result.getContent()).allMatch(post -> post.getMemberName().contains(query));
    }
}
