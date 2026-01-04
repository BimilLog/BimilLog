package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.out.GlobalMemberBlacklistAdapter;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.out.*;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisPostDetailStoreAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostQueryService 테스트</h2>
 * <p>게시글 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>게시판 조회, 인기글 조회, 검색, 캐시 처리 등의 복잡한 시나리오를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("PostQueryService 테스트")
@Tag("unit")
class PostQueryServiceTest extends BaseUnitTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostSearchRepository postSearchRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private RedisPostDetailStoreAdapter redisPostDetailStoreAdapter;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostToCommentAdapter postToCommentAdapter;

    @Mock
    private GlobalMemberBlacklistAdapter globalMemberBlacklistAdapter;

    @InjectMocks
    private PostQueryService postQueryService;

    @BeforeEach
    void setUp() {
        lenient().when(postToCommentAdapter.findCommentCountsByPostIds(anyList()))
                .thenReturn(Collections.emptyMap());
    }

    @Test
    @DisplayName("게시판 조회 - 성공")
    void shouldGetBoard_Successfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail postResult = PostTestDataBuilder.createPostSearchResult(1L, "제목1");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(postResult), pageable, 1);

        given(postQueryRepository.findByPage(pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.getBoard(pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("제목1");

        verify(postQueryRepository).findByPage(pageable, null);
    }


    @Test
    @DisplayName("게시글 상세 조회 - 일반 게시글 (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenNotPopularPost_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;
        Long postAuthorId = 1L; // 게시글 작성자 ID

        // 캐시에 없음 (인기글이 아님)
        given(redisPostDetailStoreAdapter.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리 결과
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(postAuthorId) // 게시글 작성자 ID
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .isNotice(false)
                .build();
        given(postQueryRepository.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        verify(redisPostDetailStoreAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryRepository).findPostDetailWithCounts(postId, memberId); // 1회 DB 쿼리
        verify(globalMemberBlacklistAdapter).checkMemberBlacklist(memberId, postAuthorId); // 블랙리스트 확인
        verify(postLikeRepository, never()).existsByPostIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 인기글인 경우 (캐시에서 조회, 최적화)")
    void shouldGetPost_WhenPopularPostFromCache_Optimized() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;
        Long postAuthorId = 1L; // 게시글 작성자 ID

        PostDetail cachedFullPost = PostDetail.builder()
                .id(postId)
                .title("캐시된 인기글")
                .content("캐시된 내용")
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberId(postAuthorId) // 게시글 작성자 ID
                .memberName("testMember")
                .commentCount(3)
                .isLiked(false)
                .isNotice(false)
                .build();

        // 최적화: 한번의 호출로 캐시 존재 여부와 데이터를 함께 확인
        given(redisPostDetailStoreAdapter.getCachedPostIfExists(postId)).willReturn(cachedFullPost);

        // 좋아요 정보만 추가 확인 (Post 엔티티 로드 없이 ID로만 확인)
        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(false);

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse();

        verify(redisPostDetailStoreAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(globalMemberBlacklistAdapter).checkMemberBlacklist(memberId, postAuthorId); // 블랙리스트 확인
        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
        verify(postQueryRepository, never()).findPostDetailWithCounts(any(), any()); // JOIN 쿼리도 호출 안함
    }

    @Test
    @DisplayName("게시글 상세 조회 - 캐시 miss (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenCacheMiss_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;
        Long postAuthorId = 1L; // 게시글 작성자 ID

        // 캐시에 상세 정보가 없음 (인기글이 아니거나 캐시 만료)
        given(redisPostDetailStoreAdapter.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리로 DB에서 조회
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(postAuthorId) // 게시글 작성자 ID
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .isNotice(false)
                .build();
        given(postQueryRepository.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        verify(redisPostDetailStoreAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(postQueryRepository).findPostDetailWithCounts(postId, memberId); // 1회 DB JOIN 쿼리 (최적화)
        verify(globalMemberBlacklistAdapter).checkMemberBlacklist(memberId, postAuthorId); // 블랙리스트 확인

        // 기존 개별 쿼리들은 호출되지 않음을 검증
        verify(postLikeRepository, never()).existsByPostIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 (memberId null, 최적화)")
    void shouldGetPost_WhenAnonymousUser() {
        // Given
        Long postId = 1L;
        Long memberId = null;
        Long postAuthorId = 1L; // 게시글 작성자 ID

        // 캐시에 없음 (인기글이 아니거나 캐시 만료)
        given(redisPostDetailStoreAdapter.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리로 DB에서 조회 (익명 사용자이므로 isLiked는 false)
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(postAuthorId) // 게시글 작성자 ID
                .isLiked(false) // 익명 사용자는 항상 false
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .isNotice(false)
                .build();
        given(postQueryRepository.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse(); // 익명 사용자는 항상 false

        verify(redisPostDetailStoreAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryRepository).findPostDetailWithCounts(postId, memberId); // 1회 JOIN 쿼리
        verify(globalMemberBlacklistAdapter, never()).checkMemberBlacklist(any(), any()); // 익명 사용자는 블랙리스트 체크 안함

        // 기존 개별 쿼리들은 호출되지 않음
        verify(postLikeRepository, never()).existsByPostIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글 (최적화)")
    void shouldThrowException_WhenPostNotFound_Optimized() {
        // Given
        Long postId = 999L;
        Long memberId = 1L;

        // 캐시에도 없고 DB에도 없는 경우
        given(redisPostDetailStoreAdapter.getCachedPostIfExists(postId)).willReturn(null);
        given(postQueryRepository.findPostDetailWithCounts(postId, memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPost(postId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(redisPostDetailStoreAdapter).getCachedPostIfExists(postId);
        verify(postQueryRepository).findPostDetailWithCounts(postId, memberId);
        // 기존 개별 쿼리는 호출되지 않음
    }

    @Test
    @DisplayName("게시글 검색 - 전문검색 전략 (3글자 이상 + TITLE)")
    void shouldSearchPost_UsingFullTextSearch_When3CharsOrMore() {
        // Given
        PostSearchType type = PostSearchType.TITLE;
        String query = "검색어"; // 3글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByFullTextSearch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByFullTextSearch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByPartialMatch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("게시글 검색 - 전문검색 조건 충족 시 결과 그대로 반환")
    void shouldNotFallback_WhenFullTextSearchSelected() {
        // Given
        PostSearchType type = PostSearchType.TITLE_CONTENT;
        String query = "검색어"; // 3글자
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> emptyPage = Page.empty(pageable);

        given(postSearchRepository.findByFullTextSearch(type, query, pageable, null)).willReturn(emptyPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(emptyPage);

        verify(postSearchRepository).findByFullTextSearch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByPartialMatch(type, query, pageable, null);
    }

    @Test
    @DisplayName("게시글 검색 - 접두사 검색 전략 (WRITER + 4글자 이상)")
    void shouldSearchPost_UsingPrefixMatch_WhenWriter4CharsOrMore() {
        // Given
        PostSearchType type = PostSearchType.WRITER;
        String query = "작성자이름"; // 5글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "작성자 검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByPrefixMatch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByPrefixMatch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPartialMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("게시글 검색 - 부분검색 전략 (WRITER + 4글자 미만)")
    void shouldSearchPost_UsingPartialMatch_WhenWriterLessThan4Chars() {
        // Given
        PostSearchType type = PostSearchType.WRITER;
        String query = "작성자"; // 3글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "부분 검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByPartialMatch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByPartialMatch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("게시글 검색 - 부분검색 전략 (3글자 미만)")
    void shouldSearchPost_UsingPartialMatch_WhenLessThan3Chars() {
        // Given
        PostSearchType type = PostSearchType.TITLE;
        String query = "검색"; // 2글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "부분 검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByPartialMatch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByPartialMatch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
    }


    @Test
    @DisplayName("게시글 ID로 조회 - 성공")
    void shouldFindById_WhenPostExists() {
        // Given
        Long postId = 1L;

        Post mockPost = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "Test Post", "Content"));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));

        // When
        Optional<Post> result = postQueryService.findById(postId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(postId);

        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 존재하지 않음")
    void shouldFindById_WhenPostNotExists() {
        // Given
        Long postId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When
        Optional<Post> result = postQueryService.findById(postId);

        // Then
        assertThat(result).isEmpty();
        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("사용자 작성 게시글 조회 - 성공")
    void shouldGetUserPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail userPost = PostTestDataBuilder.createPostSearchResult(1L, "사용자 게시글");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(userPost), pageable, 1);

        Page<PostSimpleDetail> emptyLikedPage = Page.empty();
        given(postQueryRepository.findPostsByMemberId(memberId, pageable, memberId)).willReturn(expectedPage);
        given(postQueryRepository.findLikedPostsByMemberId(memberId, pageable)).willReturn(emptyLikedPage);
        given(postToCommentAdapter.findCommentCountsByPostIds(List.of(1L))).willReturn(Map.of(1L, 0));

        // When
        MemberActivityPost result = postQueryService.getMemberActivityPosts(memberId, pageable);

        // Then
        assertThat(result.getWritePosts()).isEqualTo(expectedPage);
        assertThat(result.getWritePosts().getContent()).hasSize(1);

        verify(postQueryRepository).findPostsByMemberId(memberId, pageable, memberId);
    }

    @Test
    @DisplayName("사용자 추천한 게시글 조회 - 성공")
    void shouldGetUserLikedPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail likedPost = PostTestDataBuilder.createPostSearchResult(1L, "추천한 게시글");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(likedPost), pageable, 1);

        Page<PostSimpleDetail> emptyWritePage = Page.empty();
        given(postQueryRepository.findPostsByMemberId(memberId, pageable, memberId)).willReturn(emptyWritePage);
        given(postQueryRepository.findLikedPostsByMemberId(memberId, pageable)).willReturn(expectedPage);
        given(postToCommentAdapter.findCommentCountsByPostIds(List.of(1L))).willReturn(Map.of(1L, 0));

        // When
        MemberActivityPost result = postQueryService.getMemberActivityPosts(memberId, pageable);

        // Then
        assertThat(result.getLikedPosts()).isEqualTo(expectedPage);
        assertThat(result.getLikedPosts().getContent()).hasSize(1);

        verify(postQueryRepository).findLikedPostsByMemberId(memberId, pageable);
    }

    @Test
    @DisplayName("주간 인기 게시글 조회 시 댓글 수가 주입된다")
    void shouldInjectCommentCountsForWeeklyPosts() {
        // Given
        PostSimpleDetail weeklyPost = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기");
        given(postQueryRepository.findWeeklyPopularPosts()).willReturn(List.of(weeklyPost));
        given(postToCommentAdapter.findCommentCountsByPostIds(List.of(1L)))
                .willReturn(Map.of(1L, 7));

        // When
        List<PostSimpleDetail> result = postQueryService.getWeeklyPopularPosts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCommentCount()).isEqualTo(7);
        verify(postToCommentAdapter).findCommentCountsByPostIds(List.of(1L));
    }

    @Test
    @DisplayName("레전드 게시글 조회 시 댓글 수가 주입된다")
    void shouldInjectCommentCountsForLegendaryPosts() {
        // Given
        PostSimpleDetail legendaryPost = PostTestDataBuilder.createPostSearchResult(10L, "레전드");
        given(postQueryRepository.findLegendaryPosts()).willReturn(List.of(legendaryPost));
        given(postToCommentAdapter.findCommentCountsByPostIds(List.of(10L)))
                .willReturn(Map.of(10L, 3));

        // When
        List<PostSimpleDetail> result = postQueryService.getLegendaryPosts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCommentCount()).isEqualTo(3);
        verify(postToCommentAdapter).findCommentCountsByPostIds(List.of(10L));
    }

}
