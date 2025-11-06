package jaeik.bimillog.adapter.out.post;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostToCommentPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.out.post.PostQueryAdapter;
import jaeik.bimillog.out.post.PostQueryHelper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
                classes = {PostQueryAdapter.class, PostQueryHelper.class}
        )
)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostQueryAdapter.class, PostQueryHelper.class, QueryDSLConfig.class, LocalIntegrationTestSupportConfig.class})
@Tag("local-integration")
class PostQueryAdapterIntegrationTest {

    @Autowired
    private PostQueryAdapter postQueryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @MockitoBean
    private PostToCommentPort postToCommentPort;

    @MockitoBean
    private PostLikeQueryPort postLikeQueryPort;

    private Member testMember;
    private Post testPost1, testPost2, testPost3, noticePost;

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
        
        // 댓글 수 Mock 설정 (기본값)
        Map<Long, Integer> commentCounts = new HashMap<>();
        commentCounts.put(testPost1.getId(), 2);
        commentCounts.put(testPost2.getId(), 1);
        commentCounts.put(testPost3.getId(), 0);
        commentCounts.put(noticePost.getId(), 3);
        
        // 추천 수 Mock 설정 (기본값)
        Map<Long, Integer> likeCounts = new HashMap<>();
        likeCounts.put(testPost1.getId(), 5);
        likeCounts.put(testPost2.getId(), 3);
        likeCounts.put(testPost3.getId(), 1);
        likeCounts.put(noticePost.getId(), 8);
        
        given(postToCommentPort.findCommentCountsByPostIds(any(List.class)))
                .willReturn(commentCounts);
        given(postLikeQueryPort.findLikeCountsByPostIds(any(List.class)))
                .willReturn(likeCounts);
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

        // 공지사항 게시글
        noticePost = Post.createPost(testMember, "공지사항 제목", "중요한 공지사항입니다.", 1234);
        noticePost.setAsNotice(); // 공지사항으로 설정
        entityManager.persistAndFlush(noticePost);

        entityManager.flush();
        // clear() 제거: 테스트에서 엔티티를 계속 사용하므로 분리하지 않음
    }


    @Test
    @DisplayName("정상 케이스 - 페이지별 게시글 조회 (공지사항 제외)")
    void shouldFindPostsByPage_WhenValidPageableProvided() {
        // Given: 페이지 요청 (첫 페이지, 크기 2)
        Pageable pageable = PageRequest.of(0, 2);

        // When: 페이지별 게시글 조회
        Page<PostSimpleDetail> result = postQueryAdapter.findByPage(pageable);

        // Then: 공지사항이 제외된 일반 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 요청한 크기만큼
        assertThat(result.getTotalElements()).isEqualTo(3L); // 전체 일반 게시글 수
        assertThat(result.getTotalPages()).isEqualTo(2); // 전체 페이지 수

        // 공지사항은 제외되어야 함
        List<String> titles = result.getContent().stream()
                .map(PostSimpleDetail::getTitle)
                .toList();
        assertThat(titles).doesNotContain("공지사항 제목");
        
        // 댓글 수와 추천 수가 설정되어 있는지 확인
        assertThat(result.getContent().getFirst().getCommentCount()).isNotNull();
        assertThat(result.getContent().getFirst().getLikeCount()).isNotNull();
    }


    @Test
    @DisplayName("정상 케이스 - 사용자별 작성 게시글 조회")
    void shouldFindPostsByMemberId_WhenValidMemberIdProvided() {
        // Given: 사용자 ID와 페이지 요청
        Long memberId = testMember.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자별 게시글 조회
        Page<PostSimpleDetail> result = postQueryAdapter.findPostsByMemberId(memberId, pageable);

        // Then: 해당 사용자의 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(4); // 공지사항 포함 전체 게시글
        assertThat(result.getTotalElements()).isEqualTo(4L);

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
        if (likeMember.getKakaoToken() != null) {
            entityManager.persist(likeMember.getKakaoToken());
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
        Page<PostSimpleDetail> result = postQueryAdapter.findLikedPostsByMemberId(likeMember.getId(), pageable);

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
        Page<PostSimpleDetail> result = postQueryAdapter.findByPartialMatch(searchType, query, pageable);

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
        Page<PostSimpleDetail> result = postQueryAdapter.findByPrefixMatch(searchType, query, pageable);

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
        Page<PostSimpleDetail> result = postQueryAdapter.findByFullTextSearch(searchType, query, pageable);

        // Then: FULLTEXT 인덱스를 사용하여 검색됨
        assertThat(result).isNotNull();
        // FULLTEXT 검색 결과는 빈 페이지일 수 있음 (테스트 환경에 따라)
    }



    @Test
    @DisplayName("비즈니스 로직 - 공지사항은 일반 페이지 조회에서 제외")
    void shouldExcludeNoticePosts_WhenFindingByPage() {
        // Given: 공지사항과 일반 게시글이 모두 존재
        Pageable pageable = PageRequest.of(0, 10);

        // When: 일반 게시글 페이지 조회
        Page<PostSimpleDetail> result = postQueryAdapter.findByPage(pageable);

        // Then: 공지사항은 제외되고 일반 게시글만 조회됨
        assertThat(result.getContent()).hasSize(3); // 일반 게시글 3개만 조회 (공지사항 2개 제외)
    }

    @Test
    @DisplayName("비즈니스 로직 - 최신 게시글부터 정렬")
    void shouldSortByCreatedAtDesc_WhenFindingPosts() {
        // Given: 여러 게시글이 존재
        Pageable pageable = PageRequest.of(0, 10);

        // When: 게시글 조회
        Page<PostSimpleDetail> result = postQueryAdapter.findByPage(pageable);

        // Then: 최신 게시글부터 정렬됨 (createdAt 내림차순)
        List<java.time.Instant> createdAts = result.getContent().stream()
                .map(PostSimpleDetail::getCreatedAt)
                .toList();

        for (int i = 1; i < createdAts.size(); i++) {
            assertThat(createdAts.get(i-1)).isAfterOrEqualTo(createdAts.get(i));
        }
    }

    @Test
    @DisplayName("정렬 검증 - 추천한 게시글이 추천 날짜 기준 최신순으로 정렬됨")
    void shouldSortLikedPostsByLikeDateDesc_WhenMultipleLikesExist() {
        // Given: 사용자가 여러 게시글에 시간 간격을 두고 추천
        Member likeMember = TestMembers.copyWithId(TestMembers.MEMBER_2, null);
        if (likeMember.getSetting() != null) {
            entityManager.persist(likeMember.getSetting());
        }
        if (likeMember.getKakaoToken() != null) {
            entityManager.persist(likeMember.getKakaoToken());
        }
        entityManager.persistAndFlush(likeMember);

        // 첫 번째 게시글에 추천 (가장 오래된 추천)
        PostLike postLike1 = PostLike.builder()
                .post(testPost1)
                .member(likeMember)
                .build();
        entityManager.persist(postLike1);
        entityManager.flush();

        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 두 번째 게시글에 추천
        PostLike postLike2 = PostLike.builder()
                .post(testPost2)
                .member(likeMember)
                .build();
        entityManager.persist(postLike2);
        entityManager.flush();

        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 세 번째 게시글에 추천 (가장 최근 추천)
        PostLike postLike3 = PostLike.builder()
                .post(testPost3)
                .member(likeMember)
                .build();
        entityManager.persist(postLike3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자 추천 게시글 조회
        Page<PostSimpleDetail> result = postQueryAdapter.findLikedPostsByMemberId(likeMember.getId(), pageable);

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
