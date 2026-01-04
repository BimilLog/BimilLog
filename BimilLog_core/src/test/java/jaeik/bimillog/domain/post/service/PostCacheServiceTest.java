package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.out.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostCacheService 테스트</h2>
 * <p>게시글 캐시 조회 및 동기화 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheService 테스트")
@Tag("unit")
class PostCacheServiceTest {


    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;

    @Mock
    private RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;

    @Mock
    private RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;


    @InjectMocks
    private PostCacheService postCacheService;

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트")
    void shouldGetRealtimePosts() {
        // Given
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        Map<Long, PostSimpleDetail> cachedMap = new HashMap<>();
        cachedMap.put(1L, simpleDetail1);
        cachedMap.put(2L, simpleDetail2);

        given(redisRealTimePostStoreAdapter.getRealtimePopularPostIds()).willReturn(List.of(1L, 2L));
        given(redisTier1PostStoreAdapter.getCachedPostMap(PostCacheFlag.REALTIME)).willReturn(cachedMap);

        // When
        List<PostSimpleDetail> result = postCacheService.getRealtimePosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.get(1).getTitle()).isEqualTo("실시간 인기글 2");

        verify(redisRealTimePostStoreAdapter).getRealtimePopularPostIds();
        verify(redisTier1PostStoreAdapter).getCachedPostMap(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스")
    void shouldGetRealtimePosts_WhenCacheMissing() {
        // Given
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "캐시된 게시글");
        PostDetail realtimePost1 = createPostDetail(1L, "DB에서 조회된 게시글");

        Map<Long, PostSimpleDetail> cachedMap = new HashMap<>();
        cachedMap.put(2L, simpleDetail2); // postId=1은 캐시 미스

        given(redisRealTimePostStoreAdapter.getRealtimePopularPostIds()).willReturn(List.of(1L, 2L));
        given(redisTier1PostStoreAdapter.getCachedPostMap(PostCacheFlag.REALTIME)).willReturn(cachedMap);
        given(postQueryRepository.findPostDetailWithCounts(1L, null)).willReturn(Optional.of(realtimePost1)); // DB fallback

        // When
        List<PostSimpleDetail> result = postCacheService.getRealtimePosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("DB에서 조회된 게시글");
        assertThat(result.get(1).getTitle()).isEqualTo("캐시된 게시글");

        verify(redisRealTimePostStoreAdapter).getRealtimePopularPostIds();
        verify(redisTier1PostStoreAdapter).getCachedPostMap(PostCacheFlag.REALTIME);
        verify(postQueryRepository).findPostDetailWithCounts(1L, null);
        verify(redisTier1PostStoreAdapter).cachePostList(eq(PostCacheFlag.REALTIME), any());
    }

    @Test
    @DisplayName("주간 인기글 조회")
    void shouldGetWeeklyPosts() {
        // Given
        List<PostSimpleDetail> weeklyPosts = List.of(
            PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1"),
            PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2")
        );

        given(redisTier1PostStoreAdapter.getPostListCacheTTL(PostCacheFlag.WEEKLY)).willReturn(300L);  // 충분한 TTL (5분) → asyncRefreshCache 호출 안 됨
        given(redisTier1PostStoreAdapter.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        List<PostSimpleDetail> result = postCacheService.getWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("주간 인기글 1");
        assertThat(result.get(1).getTitle()).isEqualTo("주간 인기글 2");

        verify(redisTier1PostStoreAdapter).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(legendPost1, legendPost2), pageable, 2);

        given(redisTier1PostStoreAdapter.getCachedPostListPaged(pageable)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("레전드 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("레전드 게시글 2");

        verify(redisTier1PostStoreAdapter).getCachedPostListPaged(pageable);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 미스 (빈 페이지 반환)")
    void shouldGetPopularPostLegend_WhenCacheMiss() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostSimpleDetail> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(redisTier1PostStoreAdapter.getCachedPostListPaged(pageable)).willReturn(emptyPage);

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(redisTier1PostStoreAdapter).getCachedPostListPaged(pageable);
    }

    @Test
    @DisplayName("공지사항 조회 - 성공")
    void shouldGetNoticePosts_Successfully() {
        // Given
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<PostSimpleDetail> noticePosts = List.of(noticePost);

        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.NOTICE)).willReturn(List.of(1L));
        given(redisTier1PostStoreAdapter.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(noticePosts);

        // When
        List<PostSimpleDetail> result = postCacheService.getNoticePosts();

        // Then
        assertThat(result).isEqualTo(noticePosts);
        assertThat(result).hasSize(1);

        verify(redisTier2PostStoreAdapter).getStoredPostIds(PostCacheFlag.NOTICE);
        verify(redisTier1PostStoreAdapter).getCachedPostList(PostCacheFlag.NOTICE);
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 미스 (빈 리스트 반환)")
    void shouldGetWeeklyPosts_WhenCacheMiss() {
        // Given
        given(redisTier1PostStoreAdapter.getPostListCacheTTL(PostCacheFlag.WEEKLY)).willReturn(300L);
        given(redisTier1PostStoreAdapter.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(List.of());

        // When
        List<PostSimpleDetail> result = postCacheService.getWeeklyPosts();

        // Then
        assertThat(result).isEmpty();
        verify(redisTier1PostStoreAdapter).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 미스 (빈 리스트 반환, asyncRefreshCache 호출)")
    void shouldGetNoticePosts_WhenCacheMiss() {
        // Given
        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.NOTICE)).willReturn(List.of(1L));
        given(redisTier1PostStoreAdapter.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(List.of());  // 캐시 미스 (size 불일치)

        // When
        List<PostSimpleDetail> result = postCacheService.getNoticePosts();

        // Then
        assertThat(result).isEmpty();  // 빈 캐시 반환 (asyncRefreshCache는 백그라운드 실행)
        verify(redisTier2PostStoreAdapter).getStoredPostIds(PostCacheFlag.NOTICE);
        verify(redisTier1PostStoreAdapter).getCachedPostList(PostCacheFlag.NOTICE);
    }

    // Helper method for creating PostDetail test data
    private PostDetail createPostDetail(Long id, String title) {
        return PostDetail.builder()
                .id(id)
                .title(title)
                .content("테스트 내용")
                .viewCount(0)
                .likeCount(0)
                .createdAt(Instant.now())
                .memberId(1L)
                .memberName("테스트 사용자")
                .commentCount(0)
                .isLiked(false)
                .build();
    }
}
