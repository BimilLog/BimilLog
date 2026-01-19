package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.CachedPost;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisPostKeys;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostCacheService 테스트</h2>
 * <p>MGET 기반 개별 캐시 조회 및 PER 로직을 검증합니다.</p>
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

    @Mock
    private PostCacheRefresh postCacheRefresh;

    @InjectMocks
    private PostCacheService postCacheService;

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트")
    void shouldGetRealtimePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        Map<Long, CachedPost> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, new CachedPost(simpleDetail1, RedisPostKeys.POST_CACHE_TTL));
        cachedPosts.put(2L, new CachedPost(simpleDetail2, RedisPostKeys.POST_CACHE_TTL));

        given(redisRealTimePostStoreAdapter.getRealtimePopularPostCount()).willReturn(2L);
        given(redisRealTimePostStoreAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L, 2L));
        given(redisTier1PostStoreAdapter.getCachedPosts(PostCacheFlag.REALTIME, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisTier1PostStoreAdapter.filterRefreshNeeded(cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(simpleDetail1, simpleDetail2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisTier1PostStoreAdapter).getCachedPosts(PostCacheFlag.REALTIME, List.of(1L, 2L));
        verify(postCacheRefresh, never()).asyncRefreshPosts(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 시 DB 조회")
    void shouldGetRealtimePosts_CacheMiss() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "캐시된 게시글");
        PostDetail realtimePost1 = createPostDetail(1L, "DB에서 조회된 게시글");

        Map<Long, CachedPost> cachedPosts = new HashMap<>();
        cachedPosts.put(2L, new CachedPost(simpleDetail2, RedisPostKeys.POST_CACHE_TTL));

        given(redisRealTimePostStoreAdapter.getRealtimePopularPostCount()).willReturn(2L);
        given(redisRealTimePostStoreAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L, 2L));
        given(redisTier1PostStoreAdapter.getCachedPosts(PostCacheFlag.REALTIME, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisTier1PostStoreAdapter.filterRefreshNeeded(cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of(1L));
        given(postQueryRepository.findPostDetail(1L, null)).willReturn(Optional.of(realtimePost1));

        // toOrderedList는 cachedPosts가 업데이트된 후 호출됨
        PostSimpleDetail simpleDetail1 = realtimePost1.toSimpleDetail();
        given(redisTier1PostStoreAdapter.toOrderedList(eq(List.of(1L, 2L)), any())).willReturn(List.of(simpleDetail1, simpleDetail2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(postQueryRepository).findPostDetail(1L, null);
        verify(redisTier1PostStoreAdapter).cachePost(eq(PostCacheFlag.REALTIME), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - PER 트리거")
    void shouldGetRealtimePosts_PERTriggered() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        Map<Long, CachedPost> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, new CachedPost(simpleDetail1, RedisPostKeys.POST_CACHE_TTL));

        given(redisRealTimePostStoreAdapter.getRealtimePopularPostCount()).willReturn(1L);
        given(redisRealTimePostStoreAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L));
        given(redisTier1PostStoreAdapter.getCachedPosts(PostCacheFlag.REALTIME, List.of(1L))).willReturn(cachedPosts);
        given(redisTier1PostStoreAdapter.filterRefreshNeeded(cachedPosts)).willReturn(List.of(1L)); // PER 대상
        given(redisTier1PostStoreAdapter.filterMissedIds(List.of(1L), cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.toOrderedList(List.of(1L), cachedPosts)).willReturn(List.of(simpleDetail1));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh).asyncRefreshPosts(PostCacheFlag.REALTIME, List.of(1L));
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 히트")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");

        Map<Long, CachedPost> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, new CachedPost(weeklyPost1, RedisPostKeys.POST_CACHE_TTL));
        cachedPosts.put(2L, new CachedPost(weeklyPost2, RedisPostKeys.POST_CACHE_TTL));

        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.WEEKLY)).willReturn(List.of(1L, 2L));
        given(redisTier1PostStoreAdapter.getCachedPosts(PostCacheFlag.WEEKLY, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisTier1PostStoreAdapter.filterRefreshNeeded(cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(weeklyPost1, weeklyPost2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("주간 인기글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("주간 인기글 2");
    }

    @Test
    @DisplayName("주간 인기글 조회 - Tier2 비어있음")
    void shouldGetWeeklyPosts_EmptyTier2() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.WEEKLY)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");

        Map<Long, CachedPost> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, new CachedPost(legendPost1, RedisPostKeys.POST_CACHE_TTL));
        cachedPosts.put(2L, new CachedPost(legendPost2, RedisPostKeys.POST_CACHE_TTL));

        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.LEGEND)).willReturn(List.of(1L, 2L));
        given(redisTier1PostStoreAdapter.getCachedPosts(PostCacheFlag.LEGEND, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisTier1PostStoreAdapter.filterRefreshNeeded(cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(legendPost1, legendPost2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("레전드 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("레전드 게시글 2");
    }

    @Test
    @DisplayName("레전드 인기 게시글 - Tier2 비어있음")
    void shouldGetPopularPostLegend_EmptyTier2() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.LEGEND)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");

        Map<Long, CachedPost> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, new CachedPost(noticePost, RedisPostKeys.POST_CACHE_TTL));

        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.NOTICE)).willReturn(List.of(1L));
        given(redisTier1PostStoreAdapter.getCachedPosts(PostCacheFlag.NOTICE, List.of(1L))).willReturn(cachedPosts);
        given(redisTier1PostStoreAdapter.filterRefreshNeeded(cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.filterMissedIds(List.of(1L), cachedPosts)).willReturn(List.of());
        given(redisTier1PostStoreAdapter.toOrderedList(List.of(1L), cachedPosts)).willReturn(List.of(noticePost));

        // When
        Page<PostSimpleDetail> result = postCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("공지사항 조회 - Tier2 비어있음")
    void shouldGetNoticePosts_EmptyTier2() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.NOTICE)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = postCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("페이징 - offset이 전체 크기보다 큰 경우 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenOffsetExceedsTotalSize() {
        // Given
        Pageable pageable = PageRequest.of(10, 10); // offset = 100
        given(redisTier2PostStoreAdapter.getStoredPostIds(PostCacheFlag.WEEKLY)).willReturn(List.of(1L, 2L)); // 총 2개

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
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
