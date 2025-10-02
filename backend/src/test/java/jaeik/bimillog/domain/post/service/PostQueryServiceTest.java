package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostCacheSyncService;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.PostTestDataBuilder;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private PostQueryPort postQueryPort;

    @Mock
    private GlobalPostQueryPort globalPostQueryPort;

    @Mock
    private PostLikeQueryPort postLikeQueryPort;

    @Mock
    private PostCacheSyncService postCacheSyncService;

    @Mock
    private RedisPostQueryPort redisPostQueryPort;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("게시판 조회 - 성공")
    void shouldGetBoard_Successfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchResult postResult = PostTestDataBuilder.createPostSearchResult(1L, "제목1");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(postResult), pageable, 1);

        given(postQueryPort.findByPage(pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getBoard(pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("제목1");

        verify(postQueryPort).findByPage(pageable);
    }


    @Test
    @DisplayName("게시글 상세 조회 - 일반 게시글 (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenNotPopularPost_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;

        // 캐시에 없음 (인기글이 아님)
        given(redisPostQueryPort.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리 결과
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(memberId != null ? memberId : 1L)
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .isNotice(false)
                .build();
        given(postQueryPort.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        verify(redisPostQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryPort).findPostDetailWithCounts(postId, memberId); // 1회 DB 쿼리
        verify(globalPostQueryPort, never()).findById(any()); // 기존 개별 쿼리 호출 안함
        verify(postLikeQueryPort, never()).existsByPostIdAndUserId(any(), any());
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
                .postCacheFlag(PostCacheFlag.REALTIME)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("testMember")
                .commentCount(3)
                .isNotice(false)
                .isLiked(false)
                .build();

        // 최적화: 한번의 호출로 캐시 존재 여부와 데이터를 함께 확인
        given(redisPostQueryPort.getCachedPostIfExists(postId)).willReturn(cachedFullPost);

        // 좋아요 정보만 추가 확인 (Post 엔티티 로드 없이 ID로만 확인)
        given(postLikeQueryPort.existsByPostIdAndUserId(postId, memberId)).willReturn(false);

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse();

        verify(redisPostQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, memberId);
        verify(globalPostQueryPort, never()).findById(any()); // 캐시 히트 시 DB 조회 안함
        verify(postQueryPort, never()).findPostDetailWithCounts(any(), any()); // JOIN 쿼리도 호출 안함
    }

    @Test
    @DisplayName("게시글 상세 조회 - 캐시 miss (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenCacheMiss_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;

        // 캐시에 상세 정보가 없음 (인기글이 아니거나 캐시 만료)
        given(redisPostQueryPort.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리로 DB에서 조회
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(memberId != null ? memberId : 1L)
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .isNotice(false)
                .build();
        given(postQueryPort.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        verify(redisPostQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(postQueryPort).findPostDetailWithCounts(postId, memberId); // 1회 DB JOIN 쿼리 (최적화)

        // 기존 개별 쿼리들은 호출되지 않음을 검증
        verify(globalPostQueryPort, never()).findById(any());
        verify(postLikeQueryPort, never()).existsByPostIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 (memberId null, 최적화)")
    void shouldGetPost_WhenAnonymousUser() {
        // Given
        Long postId = 1L;
        Long memberId = null;

        // 캐시에 없음 (인기글이 아니거나 캐시 만료)
        given(redisPostQueryPort.getCachedPostIfExists(postId)).willReturn(null);

        // 최적화된 JOIN 쿼리로 DB에서 조회 (익명 사용자이므로 isLiked는 false)
        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("Test Title")
                .content("Test Content")
                .memberId(memberId != null ? memberId : 1L)
                .isLiked(memberId != null)
                .viewCount(10)
                .likeCount(5)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .isNotice(false)
                .build();
        given(postQueryPort.findPostDetailWithCounts(postId, memberId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse(); // 익명 사용자는 항상 false

        verify(redisPostQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryPort).findPostDetailWithCounts(postId, memberId); // 1회 JOIN 쿼리

        // 기존 개별 쿼리들은 호출되지 않음
        verify(globalPostQueryPort, never()).findById(any());
        verify(postLikeQueryPort, never()).existsByPostIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글 (최적화)")
    void shouldThrowException_WhenPostNotFound_Optimized() {
        // Given
        Long postId = 999L;
        Long memberId = 1L;

        // 캐시에도 없고 DB에도 없는 경우
        given(redisPostQueryPort.getCachedPostIfExists(postId)).willReturn(null);
        given(postQueryPort.findPostDetailWithCounts(postId, memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPost(postId, memberId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(redisPostQueryPort).getCachedPostIfExists(postId);
        verify(postQueryPort).findPostDetailWithCounts(postId, memberId);
        // 기존 개별 쿼리는 호출되지 않음
        verify(globalPostQueryPort, never()).findById(any());
    }

    @Test
    @DisplayName("게시글 검색 - 성공")
    void shouldSearchPost_Successfully() {
        // Given
        PostSearchType type = PostSearchType.TITLE;
        String query = "검색어";
        Pageable pageable = PageRequest.of(0, 10);

        PostSearchResult searchResult = PostTestDataBuilder.createPostSearchResult(1L, "검색 결과");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postQueryPort.findBySearch(type, query, pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryPort).findBySearch(type, query, pageable);
    }


    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 있음")
    void shouldGetPopularPostLegend_WhenCacheExists() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable pageable = PageRequest.of(0, 10);

        PostSearchResult legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSearchResult legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(legendPost1, legendPost2), pageable, 2);

        given(redisPostQueryPort.hasPopularPostsCache(type)).willReturn(true);
        given(redisPostQueryPort.getCachedPostListPaged(pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getPopularPostLegend(type, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("레전드 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("레전드 게시글 2");

        verify(redisPostQueryPort).hasPopularPostsCache(type);
        verify(redisPostQueryPort).getCachedPostListPaged(pageable);
        verify(postCacheSyncService, never()).updateLegendaryPosts();
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 없음, 업데이트 후 조회")
    void shouldGetPopularPostLegend_WhenNoCacheUpdateThenGet() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable pageable = PageRequest.of(0, 5);

        PostSearchResult legendPost = PostTestDataBuilder.createPostSearchResult(1L, "업데이트된 레전드 게시글");
        Page<PostSearchResult> updatedPage = new PageImpl<>(List.of(legendPost), pageable, 1);

        given(redisPostQueryPort.hasPopularPostsCache(type)).willReturn(false);
        given(redisPostQueryPort.getCachedPostListPaged(pageable)).willReturn(updatedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getPopularPostLegend(type, pageable);

        // Then
        assertThat(result).isEqualTo(updatedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("업데이트된 레전드 게시글");

        verify(redisPostQueryPort).hasPopularPostsCache(type);
        verify(postCacheSyncService).updateLegendaryPosts();
        verify(redisPostQueryPort).getCachedPostListPaged(pageable);
    }


    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 잘못된 타입으로 호출시 예외 발생")
    void shouldThrowException_WhenGetPopularPostLegendWithNonLegendType() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME; // LEGEND가 아닌 타입
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPopularPostLegend(type, pageable))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.INVALID_INPUT_VALUE);

        // 타입 검증에서 바로 예외가 발생하므로 다른 메서드들은 호출되지 않음
        verifyNoInteractions(redisPostQueryPort);
        verifyNoInteractions(postCacheSyncService);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - null 타입으로 호출시 예외 발생")
    void shouldThrowException_WhenGetPopularPostLegendWithNullType() {
        // Given
        PostCacheFlag type = null;
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPopularPostLegend(type, pageable))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.INVALID_INPUT_VALUE);

        verifyNoInteractions(redisPostQueryPort);
        verifyNoInteractions(postCacheSyncService);
    }

    @Test
    @DisplayName("공지사항 조회 - 성공")
    void shouldGetNoticePosts_Successfully() {
        // Given
        PostSearchResult noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<PostSearchResult> noticePosts = List.of(noticePost);

        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(noticePosts);

        // When
        List<PostSearchResult> result = postQueryService.getNoticePosts();

        // Then
        assertThat(result).isEqualTo(noticePosts);
        assertThat(result).hasSize(1);

        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.NOTICE);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 성공")
    void shouldFindById_WhenPostExists() {
        // Given
        Long postId = 1L;

        Post mockPost = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "Test Post", "Content"));
        given(globalPostQueryPort.findById(postId)).willReturn(mockPost);

        // When
        Post result = postQueryService.findById(postId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);

        verify(globalPostQueryPort).findById(postId);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 존재하지 않음")
    void shouldFindById_WhenPostNotExists() {
        // Given
        Long postId = 999L;

        given(globalPostQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postQueryService.findById(postId))
                .isInstanceOf(PostCustomException.class)
                .hasMessage(PostErrorCode.POST_NOT_FOUND.getMessage());

        verify(globalPostQueryPort).findById(postId);
    }

    @Test
    @DisplayName("사용자 작성 게시글 조회 - 성공")
    void shouldGetUserPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchResult userPost = PostTestDataBuilder.createPostSearchResult(1L, "사용자 게시글");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(userPost), pageable, 1);

        given(postQueryPort.findPostsByMemberId(memberId, pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getMemberPosts(memberId, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryPort).findPostsByMemberId(memberId, pageable);
    }

    @Test
    @DisplayName("사용자 추천한 게시글 조회 - 성공")
    void shouldGetUserLikedPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchResult likedPost = PostTestDataBuilder.createPostSearchResult(1L, "추천한 게시글");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(likedPost), pageable, 1);

        given(postQueryPort.findLikedPostsByMemberId(memberId, pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getMemberLikedPosts(memberId, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postQueryPort).findLikedPostsByMemberId(memberId, pageable);
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 캐시 모두 있음")
    void shouldGetRealtimeAndWeeklyPosts_WhenBothCachesExist() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(
            PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1"),
            PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2")
        );
        List<PostSearchResult> weeklyPosts = List.of(
            PostTestDataBuilder.createPostSearchResult(3L, "주간 인기글 1"),
            PostTestDataBuilder.createPostSearchResult(4L, "주간 인기글 2")
        );

        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(true);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("realtime", "weekly");
        assertThat(result.get("realtime")).hasSize(2);
        assertThat(result.get("weekly")).hasSize(2);
        assertThat(result.get("realtime").get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.get("weekly").get(0).getTitle()).isEqualTo("주간 인기글 1");

        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService, never()).updateRealtimePopularPosts();
        verify(postCacheSyncService, never()).updateWeeklyPopularPosts();
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 실시간 캐시 없음")
    void shouldGetRealtimeAndWeeklyPosts_WhenRealtimeCacheMissing() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(PostTestDataBuilder.createPostSearchResult(1L, "업데이트된 실시간"));
        List<PostSearchResult> weeklyPosts = List.of(PostTestDataBuilder.createPostSearchResult(2L, "기존 주간"));

        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(false);
        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(true);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("realtime")).hasSize(1);
        assertThat(result.get("weekly")).hasSize(1);

        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService).updateRealtimePopularPosts();
        verify(postCacheSyncService, never()).updateWeeklyPopularPosts();
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 주간 캐시 없음")
    void shouldGetRealtimeAndWeeklyPosts_WhenWeeklyCacheMissing() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(PostTestDataBuilder.createPostSearchResult(1L, "기존 실시간"));
        List<PostSearchResult> weeklyPosts = List.of(PostTestDataBuilder.createPostSearchResult(2L, "업데이트된 주간"));

        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(false);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("realtime")).hasSize(1);
        assertThat(result.get("weekly")).hasSize(1);

        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService, never()).updateRealtimePopularPosts();
        verify(postCacheSyncService).updateWeeklyPopularPosts();
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 두 캐시 모두 없음")
    void shouldGetRealtimeAndWeeklyPosts_WhenBothCachesMissing() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(PostTestDataBuilder.createPostSearchResult(1L, "새로운 실시간"));
        List<PostSearchResult> weeklyPosts = List.of(PostTestDataBuilder.createPostSearchResult(2L, "새로운 주간"));

        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(false);
        given(redisPostQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(false);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(redisPostQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("realtime")).hasSize(1);
        assertThat(result.get("weekly")).hasSize(1);

        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService).updateRealtimePopularPosts();
        verify(postCacheSyncService).updateWeeklyPopularPosts();
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(redisPostQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

}