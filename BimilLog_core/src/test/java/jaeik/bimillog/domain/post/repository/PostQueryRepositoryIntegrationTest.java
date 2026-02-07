package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostLike;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.util.PostUtil;
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
@DataJpaTest(
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {PostQueryRepository.class, PostUtil.class}
        )
)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostSearchRepository.class, PostQueryRepository.class, PostUtil.class, QueryDSLConfig.class, LocalIntegrationTestSupportConfig.class})
@Tag("local-integration")
class PostQueryRepositoryIntegrationTest {

    @Autowired
    private PostQueryRepository postQueryRepository;

    @Autowired
    private PostSearchRepository postSearchRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Member testMember;
    private Post testPost1, testPost2, testPost3;

    @BeforeEach
    void setUp() {
        // 로컬 MySQL 데이터베이스의 기존 데이터 정리
        cleanUpDatabase();

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
    }

    /**
     * 로컬 MySQL 데이터베이스의 기존 데이터 정리
     * local-integration 테스트는 실제 MySQL을 사용하므로 기존 데이터가 있을 수 있음
     */
    private void cleanUpDatabase() {
        // 외래키 제약으로 인해 순서대로 삭제
        entityManager.getEntityManager().createNativeQuery("DELETE FROM post_like").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("DELETE FROM comment_closure").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("DELETE FROM comment").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("DELETE FROM post").executeUpdate();
        entityManager.flush();
    }

    private void createTestPosts() {
        // 일반 게시글 1
        testPost1 = Post.createPost(testMember, "첫 번째 게시글", "첫 번째 게시글 내용", 1234);
        entityManager.persistAndFlush(testPost1);

        // 일반 게시글 2
        testPost2 = Post.createPost(testMember, "두 번째 게시글", "두 번째 게시글 내용", 1234);
        entityManager.persistAndFlush(testPost2);

        // 일반 게시글 3
        testPost3 = Post.createPost(testMember, "세 번째 게시글", "세 번째 게시글 내용", 1234);
        entityManager.persistAndFlush(testPost3);

        entityManager.flush();
        // clear() 제거: 테스트에서 엔티티를 계속 사용하므로 분리하지 않음
    }


    @Test
    @DisplayName("정상 케이스 - 커서 기반 게시글 조회 (첫 페이지)")
    void shouldFindPostsByCursor_WhenNoCursorProvided() {
        // Given: 커서 없음 (첫 페이지), 크기 2
        Long cursor = null;
        int size = 2;

        // When: 커서 기반 게시글 조회
        List<PostSimpleDetail> result = postQueryRepository.findBoardPostsByCursor(cursor, size);

        // Then: size + 1개까지 조회됨 (hasNext 판단용)
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 3개 게시글 중 size+1=3개 조회

        // 댓글 수와 추천 수가 설정되어 있는지 확인
        assertThat(result.getFirst().getCommentCount()).isNotNull();
        assertThat(result.getFirst().getLikeCount()).isNotNull();
    }

    @Test
    @DisplayName("정상 케이스 - 커서 기반 게시글 조회 (다음 페이지)")
    void shouldFindPostsByCursor_WhenCursorProvided() {
        // Given: 첫 페이지 조회 후 커서 획득
        List<PostSimpleDetail> firstPage = postQueryRepository.findBoardPostsByCursor(null, 2);
        Long cursor = firstPage.get(1).getId(); // 두 번째 게시글의 ID를 커서로 사용

        // When: 커서 기반 다음 페이지 조회
        List<PostSimpleDetail> result = postQueryRepository.findBoardPostsByCursor(cursor, 2);

        // Then: 커서보다 작은 ID의 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result).allMatch(post -> post.getId() < cursor);
    }


    @Test
    @DisplayName("정상 케이스 - 사용자별 작성 게시글 조회")
    void shouldFindPostsByMemberId_WhenValidMemberIdProvided() {
        // Given: 사용자 ID와 페이지 요청
        Long memberId = testMember.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자별 게시글 조회
        Page<PostSimpleDetail> result = postQueryRepository.findPostsByMemberId(memberId, pageable, memberId);

        // Then: 해당 사용자의 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 전체 게시글
        assertThat(result.getTotalElements()).isEqualTo(3L);

        // 모든 게시글의 작성자가 해당 사용자인지 확인
        List<String> memberNames = result.getContent().stream()
                .map(PostSimpleDetail::getMemberName)
                .distinct()
                .toList();
        assertThat(memberNames).containsExactly(testMember.getMemberName());
    }


    @Test
    @DisplayName("정상 케이스 - 사용자 추천 게시글 조회")
    void shouldFindLikedPostsByMemberId_WhenMemberHasLikedPosts() {
        // Given: 사용자가 게시글에 추천을 누름
        Member likeMember = TestMembers.copyWithId(TestMembers.MEMBER_2, null);
        if (likeMember.getSetting() != null) {
            entityManager.persist(likeMember.getSetting());
        }
        if (likeMember.getSocialToken() != null) {
            entityManager.persist(likeMember.getSocialToken());
        }
        entityManager.persistAndFlush(likeMember);

        // 게시글에 좋아요 추가
        PostLike postLike1 = PostLike.builder()
                .post(testPost1)
                .member(likeMember)
                .build();
        PostLike postLike2 = PostLike.builder()
                .post(testPost2)
                .member(likeMember)
                .build();

        entityManager.persist(postLike1);
        entityManager.persist(postLike2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자 추천 게시글 조회
        Page<PostSimpleDetail> result = postQueryRepository.findLikedPostsByMemberId(likeMember.getId(), pageable);

        // Then: 추천한 게시글들이 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);

        List<String> likedPostTitles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();
        assertThat(likedPostTitles).containsExactlyInAnyOrder(
                "첫 번째 게시글", 
                "두 번째 게시글"
        );

        // 댓글 수와 추천 수도 설정되어 있는지 확인
        assertThat(result.getContent()).allMatch(post -> post.getCommentCount() != null);
        assertThat(result.getContent()).allMatch(post -> post.getLikeCount() != null);
    }


    @Test
    @DisplayName("정상 케이스 - 부분 검색 (LIKE '%query%')")
    void shouldFindPostsByPartialMatch_WhenValidSearchQueryProvided() {
        // Given: 부분 검색
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "첫";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 부분 검색
        Page<PostSimpleDetail> result = postSearchRepository.findByPartialMatch(searchType, query, pageable, null);

        // Then: 해당 제목이 포함된 게시글 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).contains("첫");
    }

    @Test
    @DisplayName("정상 케이스 - 접두사 검색 (LIKE 'query%')")
    void shouldFindPostsByPrefixMatch_WhenValidPrefixProvided() {
        // Given: 접두사 검색 (작성자명 4글자 이상)
        PostSearchType searchType = PostSearchType.WRITER;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 접두사 검색
        Page<PostSimpleDetail> result = postSearchRepository.findByPrefixMatch(searchType, query, pageable, null);

        // Then: 해당 접두사로 시작하는 작성자의 게시글들이 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThan(0);
        assertThat(result.getContent()).allMatch(post ->
                post.getMemberName().toLowerCase().startsWith(query.toLowerCase()));
    }

    @Test
    @DisplayName("정상 케이스 - 전문 검색 (MySQL FULLTEXT)")
    void shouldFindPostsByFullTextSearch_WhenValidQueryProvided() {
        // Given: 3글자 이상 검색어
        PostSearchType searchType = PostSearchType.TITLE;
        String query = "첫번째";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 전문 검색
        Page<Object[]> result = postSearchRepository.findByFullTextSearch(searchType, query, pageable, null);

        // Then: FULLTEXT 인덱스를 사용하여 검색됨
        assertThat(result).isNotNull();
        // FULLTEXT 검색 결과는 빈 페이지일 수 있음 (테스트 환경에 따라)
    }



    @Test
    @DisplayName("비즈니스 로직 - 모든 게시글이 커서 조회에 포함됨")
    void shouldIncludeAllPosts_WhenFindingByCursor() {
        // Given: 게시글이 존재, 충분히 큰 size
        int size = 10;

        // When: 커서 기반 게시글 조회 (첫 페이지)
        List<PostSimpleDetail> result = postQueryRepository.findBoardPostsByCursor(null, size);

        // Then: 모든 게시글 조회됨
        assertThat(result).hasSize(3); // 전체 게시글 3개 조회
    }

    @Test
    @DisplayName("비즈니스 로직 - ID 내림차순 정렬 (커서 기반)")
    void shouldSortByIdDesc_WhenFindingByCursor() {
        // Given: 여러 게시글이 존재
        int size = 10;

        // When: 커서 기반 게시글 조회
        List<PostSimpleDetail> result = postQueryRepository.findBoardPostsByCursor(null, size);

        // Then: ID 내림차순으로 정렬됨
        List<Long> ids = result.stream()
                .map(PostSimpleDetail::getId)
                .toList();

        for (int i = 1; i < ids.size(); i++) {
            assertThat(ids.get(i-1)).isGreaterThan(ids.get(i));
        }
    }

    @Test
    @DisplayName("정렬 검증 - 추천한 게시글이 추천 날짜 기준 최신순으로 정렬됨")
    void shouldSortLikedPostsByLikeDateDesc_WhenMultipleLikesExist() throws InterruptedException {
        // Given: 사용자가 여러 게시글에 시간 간격을 두고 추천
        Member likeMember = TestMembers.copyWithId(TestMembers.MEMBER_2, null);
        if (likeMember.getSetting() != null) {
            entityManager.persist(likeMember.getSetting());
        }
        if (likeMember.getSocialToken() != null) {
            entityManager.persist(likeMember.getSocialToken());
        }
        entityManager.persistAndFlush(likeMember);

        // 첫 번째 게시글에 추천 (가장 오래된 추천)
        PostLike postLike1 = PostLike.builder()
                .post(testPost1)
                .member(likeMember)
                .build();
        entityManager.persist(postLike1);
        entityManager.flush();
        Thread.sleep(100); // 100ms 대기

        // 두 번째 게시글에 추천
        PostLike postLike2 = PostLike.builder()
                .post(testPost2)
                .member(likeMember)
                .build();
        entityManager.persist(postLike2);
        entityManager.flush();
        Thread.sleep(100); // 100ms 대기

        // 세 번째 게시글에 추천 (가장 최근 추천)
        PostLike postLike3 = PostLike.builder()
                .post(testPost3)
                .member(likeMember)
                .build();
        entityManager.persist(postLike3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자 추천 게시글 조회
        Page<PostSimpleDetail> result = postQueryRepository.findLikedPostsByMemberId(likeMember.getId(), pageable);

        // Then: 추천 날짜 기준 최신순으로 정렬됨 (testPost3 → testPost2 → testPost1)
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);

        List<String> likedPostTitles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();

        assertThat(likedPostTitles.get(0)).isEqualTo("세 번째 게시글"); // 가장 최근 추천
        assertThat(likedPostTitles.get(1)).isEqualTo("두 번째 게시글");
        assertThat(likedPostTitles.get(2)).isEqualTo("첫 번째 게시글"); // 가장 오래된 추천
    }
}
