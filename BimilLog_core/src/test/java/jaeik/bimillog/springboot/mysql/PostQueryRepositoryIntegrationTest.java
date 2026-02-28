package jaeik.bimillog.springboot.mysql;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostLike;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.repository.PostSearchRepository;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.LocalIntegrationTestSupportConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostQueryAdapter 통합 테스트</h2>
 * <p>게시글 조회 어댑터의 핵심 비즈니스 로직을 테스트합니다.</p>
 * <p>페이징 조회, 사용자별 조회, 검색, 공지사항 제외 등</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("local-integration")
@ActiveProfiles("local-integration")
@Import(LocalIntegrationTestSupportConfig.class)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostQueryRepositoryIntegrationTest {

    @Autowired
    private PostQueryRepository postQueryRepository;

    @Autowired
    private PostSearchRepository postSearchRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private PostToMemberAdapter postToMemberAdapter;

    private Member testMember;
    private Post testPost1, testPost2, testPost3;

    /**
     * 로컬 MySQL 데이터베이스의 기존 데이터를 테스트 시작 전 1회만 정리
     * @Transactional이 각 테스트 후 자동 롤백하므로 매번 정리할 필요 없음
     * FULLTEXT(ngram) 인덱스가 있는 테이블의 DELETE는 매우 느리므로 반복 실행을 피함
     */
    @BeforeAll
    void cleanUpOnce() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM post_like");
            stmt.execute("DELETE FROM comment_closure");
            stmt.execute("DELETE FROM comment");
            stmt.execute("DELETE FROM post");
        }
    }

    @BeforeEach
    void setUp() {
        testMember = TestMembers.copyWithId(TestMembers.MEMBER_1, null);
        if (testMember.getSetting() != null) {
            entityManager.persist(testMember.getSetting());
        }
        if (testMember.getSocialToken() != null) {
            entityManager.persist(testMember.getSocialToken());
        }
        entityManager.persist(testMember);
        entityManager.flush();

        createTestPosts();
    }

    private void createTestPosts() {
        testPost1 = Post.createPost(testMember, "첫 번째 게시글", "첫 번째 게시글 내용", 1234, testMember.getMemberName());
        entityManager.persist(testPost1);
        entityManager.flush();

        testPost2 = Post.createPost(testMember, "두 번째 게시글", "두 번째 게시글 내용", 1234, testMember.getMemberName());
        entityManager.persist(testPost2);
        entityManager.flush();

        testPost3 = Post.createPost(testMember, "세 번째 게시글", "세 번째 게시글 내용", 1234, testMember.getMemberName());
        entityManager.persist(testPost3);
        entityManager.flush();
    }

    @Test
    @DisplayName("정상 케이스 - 커서 기반 게시글 조회 (첫 페이지)")
    void shouldFindPostsByCursor_WhenNoCursorProvided() {
        Long cursor = null;
        int size = 2;

        List<PostSimpleDetail> result = postQueryRepository.findBoardPostsByCursor(cursor, size);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.getFirst().getCommentCount()).isNotNull();
        assertThat(result.getFirst().getLikeCount()).isNotNull();
    }

    @Test
    @DisplayName("정상 케이스 - 커서 기반 게시글 조회 (다음 페이지)")
    void shouldFindPostsByCursor_WhenCursorProvided() {
        List<PostSimpleDetail> firstPage = postQueryRepository.findBoardPostsByCursor(null, 2);
        Long cursor = firstPage.get(1).getId();

        List<PostSimpleDetail> result = postQueryRepository.findBoardPostsByCursor(cursor, 2);

        assertThat(result).isNotNull();
        assertThat(result).allMatch(post -> post.getId() < cursor);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자별 작성 게시글 조회")
    void shouldFindPostsByMemberId_WhenValidMemberIdProvided() {
        Long memberId = testMember.getId();
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postQueryRepository.selectPostSimpleDetails(
                PostQueryType.MEMBER_POSTS.getMemberConditionFn().apply(memberId),
                pageable,
                PostQueryType.MEMBER_POSTS.getOrders());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        List<String> memberNames = result.getContent().stream()
                .map(PostSimpleDetail::getMemberName)
                .distinct()
                .toList();
        assertThat(memberNames).containsExactly(testMember.getMemberName());
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 추천 게시글 조회")
    void shouldFindLikedPostsByMemberId_WhenMemberHasLikedPosts() {
        Member likeMember = TestMembers.copyWithId(TestMembers.MEMBER_2, null);
        if (likeMember.getSetting() != null) {
            entityManager.persist(likeMember.getSetting());
        }
        if (likeMember.getSocialToken() != null) {
            entityManager.persist(likeMember.getSocialToken());
        }
        entityManager.persist(likeMember);
        entityManager.flush();

        PostLike postLike1 = PostLike.builder().post(testPost1).member(likeMember).build();
        PostLike postLike2 = PostLike.builder().post(testPost2).member(likeMember).build();
        entityManager.persist(postLike1);
        entityManager.persist(postLike2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postQueryRepository.findLikedPostsByMemberId(likeMember.getId(), pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        List<String> likedPostTitles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();
        assertThat(likedPostTitles).containsExactlyInAnyOrder("첫 번째 게시글", "두 번째 게시글");
        assertThat(result.getContent()).allMatch(post -> post.getCommentCount() != null);
        assertThat(result.getContent()).allMatch(post -> post.getLikeCount() != null);
    }

    @Test
    @DisplayName("정상 케이스 - 부분 검색 (LIKE '%query%')")
    void shouldFindPostsByPartialMatch_WhenValidSearchQueryProvided() {
        PostQueryType searchType = PostQueryType.TITLE;
        String query = "첫";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchRepository.findByPartialMatch(searchType, query, pageable, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).contains("첫");
    }

    @Test
    @DisplayName("정상 케이스 - 접두사 검색 (LIKE 'query%')")
    void shouldFindPostsByPrefixMatch_WhenValidPrefixProvided() {
        PostQueryType searchType = PostQueryType.WRITER;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> result = postSearchRepository.findByPrefixMatch(searchType, query, pageable, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThan(0);
        assertThat(result.getContent()).allMatch(post ->
                post.getMemberName().toLowerCase().startsWith(query.toLowerCase()));
    }

    @Test
    @DisplayName("정상 케이스 - 전문 검색 (MySQL FULLTEXT)")
    void shouldFindPostsByFullTextSearch_WhenValidQueryProvided() {
        PostQueryType searchType = PostQueryType.TITLE;
        String query = "첫번째";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Object[]> result = postSearchRepository.findByFullTextSearch(searchType, query, pageable, null);

        assertThat(result).isNotNull();
    }
}
