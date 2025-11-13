package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.out.GlobalPostQueryAdapter;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.post.out.PostLikeQueryAdapter;
import jaeik.bimillog.domain.post.out.PostQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostSaveAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    private PostQueryAdapter postQueryAdapter;

    @Mock
    private GlobalPostQueryAdapter globalPostQueryAdapter;

    @Mock
    private PostLikeQueryAdapter postLikeQueryAdapter;

    @Mock
    private RedisPostQueryAdapter redisPostQueryAdapter;

    @Mock
    private RedisPostSaveAdapter redisPostSaveAdapter;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("게시판 조회 - 성공")
    void shouldGetBoard_Successfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail postResult = PostTestDataBuilder.createPostSearchResult(1L, "제목1");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(postResult), pageable, 1);

        given(postQueryAdapter.findByPage(pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.getBoard(pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("제목1");

        verify(postQueryAdapter).findByPage(pageable);
    }


    @Test
    @DisplayName("게시글 상세 조회 - 일반 게시글 (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenNotPopularPost_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;

        // 캐시에 없음 (인기글이 아님)
        given(redisPostQueryAdapter.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리 결과
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(memberId != null ? memberId : 1L)
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .build();
        given(postQueryAdapter.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        verify(redisPostQueryAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryAdapter).findPostDetailWithCounts(postId, memberId); // 1회 DB 쿼리
        verify(globalPostQueryAdapter, never()).findById(any()); // 기존 개별 쿼리 호출 안함
        verify(postLikeQueryAdapter, never()).existsByPostIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 인기글인 경우 (캐시에서 조회, 최적화)")
    void shouldGetPost_WhenPopularPostFromCache_Optimized() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;

        PostDetail cachedFullPost = PostDetail.builder()
                .id(postId)
                .title("캐시된 인기글")
                .content("캐시된 내용")
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("testMember")
                .commentCount(3)
                .isLiked(false)
                .build();

        // 최적화: 한번의 호출로 캐시 존재 여부와 데이터를 함께 확인
        given(redisPostQueryAdapter.getCachedPostIfExists(postId)).willReturn(cachedFullPost);

        // 좋아요 정보만 추가 확인 (Post 엔티티 로드 없이 ID로만 확인)
        given(postLikeQueryAdapter.existsByPostIdAndUserId(postId, memberId)).willReturn(false);

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse();

        verify(redisPostQueryAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(postLikeQueryAdapter).existsByPostIdAndUserId(postId, memberId);
        verify(globalPostQueryAdapter, never()).findById(any()); // 캐시 히트 시 DB 조회 안함
        verify(postQueryAdapter, never()).findPostDetailWithCounts(any(), any()); // JOIN 쿼리도 호출 안함
    }

    @Test
    @DisplayName("게시글 상세 조회 - 캐시 miss (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenCacheMiss_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;

        // 캐시에 상세 정보가 없음 (인기글이 아니거나 캐시 만료)
        given(redisPostQueryAdapter.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리로 DB에서 조회
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(memberId != null ? memberId : 1L)
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .build();
        given(postQueryAdapter.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        verify(redisPostQueryAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(postQueryAdapter).findPostDetailWithCounts(postId, memberId); // 1회 DB JOIN 쿼리 (최적화)

        // 기존 개별 쿼리들은 호출되지 않음을 검증
        verify(globalPostQueryAdapter, never()).findById(any());
        verify(postLikeQueryAdapter, never()).existsByPostIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 (memberId null, 최적화)")
    void shouldGetPost_WhenAnonymousUser() {
        // Given
        Long postId = 1L;
        Long memberId = null;

        // 캐시에 없음 (인기글이 아니거나 캐시 만료)
        given(redisPostQueryAdapter.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리로 DB에서 조회 (익명 사용자이므로 isLiked는 false)
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(memberId != null ? memberId : 1L)
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .build();
        given(postQueryAdapter.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse(); // 익명 사용자는 항상 false

        verify(redisPostQueryAdapter).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryAdapter).findPostDetailWithCounts(postId, memberId); // 1회 JOIN 쿼리

        // 기존 개별 쿼리들은 호출되지 않음
        verify(globalPostQueryAdapter, never()).findById(any());
        verify(postLikeQueryAdapter, never()).existsByPostIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글 (최적화)")
    void shouldThrowException_WhenPostNotFound_Optimized() {
        // Given
        Long postId = 999L;
        Long memberId = 1L;

        // 캐시에도 없고 DB에도 없는 경우
        given(redisPostQueryAdapter.getCachedPostIfExists(postId)).willReturn(null);
        given(postQueryAdapter.findPostDetailWithCounts(postId, memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPost(postId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(redisPostQueryAdapter).getCachedPostIfExists(postId);
        verify(postQueryAdapter).findPostDetailWithCounts(postId, memberId);
        // 기존 개별 쿼리는 호출되지 않음
        verify(globalPostQueryAdapter, never()).findById(any());
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

        given(postQueryAdapter.findByFullTextSearch(type, query, pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryAdapter).findByFullTextSearch(type, query, pageable);
        verify(postQueryAdapter, never()).findByPartialMatch(any(), any(), any());
        verify(postQueryAdapter, never()).findByPrefixMatch(any(), any(), any());
    }

    @Test
    @DisplayName("게시글 검색 - 전문검색 실패 시 부분검색 폴백")
    void shouldSearchPost_FallbackToPartialMatch_WhenFullTextSearchFails() {
        // Given
        PostSearchType type = PostSearchType.TITLE_CONTENT;
        String query = "검색어"; // 3글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "폴백 결과");
        Page<PostSimpleDetail> emptyPage = Page.empty(pageable);
        Page<PostSimpleDetail> fallbackPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postQueryAdapter.findByFullTextSearch(type, query, pageable)).willReturn(emptyPage);
        given(postQueryAdapter.findByPartialMatch(type, query, pageable)).willReturn(fallbackPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result).isEqualTo(fallbackPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryAdapter).findByFullTextSearch(type, query, pageable);
        verify(postQueryAdapter).findByPartialMatch(type, query, pageable);
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

        given(postQueryAdapter.findByPrefixMatch(type, query, pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryAdapter).findByPrefixMatch(type, query, pageable);
        verify(postQueryAdapter, never()).findByFullTextSearch(any(), any(), any());
        verify(postQueryAdapter, never()).findByPartialMatch(any(), any(), any());
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

        given(postQueryAdapter.findByPartialMatch(type, query, pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryAdapter).findByPartialMatch(type, query, pageable);
        verify(postQueryAdapter, never()).findByFullTextSearch(any(), any(), any());
        verify(postQueryAdapter, never()).findByPrefixMatch(any(), any(), any());
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

        given(postQueryAdapter.findByPartialMatch(type, query, pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryAdapter).findByPartialMatch(type, query, pageable);
        verify(postQueryAdapter, never()).findByFullTextSearch(any(), any(), any());
        verify(postQueryAdapter, never()).findByPrefixMatch(any(), any(), any());
    }


    @Test
    @DisplayName("게시글 ID로 조회 - 성공")
    void shouldFindById_WhenPostExists() {
        // Given
        Long postId = 1L;

        Post mockPost = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "Test Post", "Content"));
        given(globalPostQueryAdapter.findById(postId)).willReturn(mockPost);

        // When
        Post result = postQueryService.findById(postId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);

        verify(globalPostQueryAdapter).findById(postId);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 존재하지 않음")
    void shouldFindById_WhenPostNotExists() {
        // Given
        Long postId = 999L;

        given(globalPostQueryAdapter.findById(postId)).willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postQueryService.findById(postId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());

        verify(globalPostQueryAdapter).findById(postId);
    }

    @Test
    @DisplayName("사용자 작성 게시글 조회 - 성공")
    void shouldGetUserPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail userPost = PostTestDataBuilder.createPostSearchResult(1L, "사용자 게시글");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(userPost), pageable, 1);

        given(postQueryAdapter.findPostsByMemberId(memberId, pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.getMemberPosts(memberId, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryAdapter).findPostsByMemberId(memberId, pageable);
    }

    @Test
    @DisplayName("사용자 추천한 게시글 조회 - 성공")
    void shouldGetUserLikedPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail likedPost = PostTestDataBuilder.createPostSearchResult(1L, "추천한 게시글");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(likedPost), pageable, 1);

        given(postQueryAdapter.findLikedPostsByMemberId(memberId, pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.getMemberLikedPosts(memberId, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryAdapter).findLikedPostsByMemberId(memberId, pageable);
    }

}