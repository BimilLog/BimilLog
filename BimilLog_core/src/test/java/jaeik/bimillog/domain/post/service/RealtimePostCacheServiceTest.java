package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>RealtimePostCacheService 테스트</h2>
 * <p>실시간 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>Hash 캐시 조회, 캐시 미스 시 빈 페이지 반환, HASH-ZSET 비교 로직을 검증합니다.</p>
 * <p>서킷브레이커 폴백은 {@code @CircuitBreaker} 어노테이션 기반이므로 통합 테스트에서 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealtimePostCacheService 테스트")
@Tag("unit")
class RealtimePostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private RealtimePostSync realtimePostSync;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Mock
    private PostUtil postUtil;

    private RealtimePostCacheService realtimePostCacheService;

    @BeforeEach
    void setUp() {
        // 기본: ZSET 조회 시 빈 리스트 반환 (비교 로직에서 사용)
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of());

        realtimePostCacheService = new RealtimePostCacheService(
                postQueryRepository,
                redisSimplePostAdapter,
                redisRealTimePostAdapter,
                realtimePostSync,
                realtimeScoreFallbackStore,
                postUtil
        );
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 + HASH-ZSET ID 일치 → 비동기 갱신 없음")
    void shouldGetRealtimePosts_CacheHit_IdsMatch() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");
        List<PostSimpleDetail> cachedPosts = List.of(simpleDetail2, simpleDetail1);

        given(redisSimplePostAdapter.getAllCachedPostsList(RedisKey.REALTIME_SIMPLE_KEY))
                .willReturn(cachedPosts);
        given(redisRealTimePostAdapter.getRangePostId())
                .willReturn(List.of(2L, 1L));
        given(postUtil.paginate(cachedPosts, pageable))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisSimplePostAdapter).getAllCachedPostsList(RedisKey.REALTIME_SIMPLE_KEY);
        verify(realtimePostSync, never()).asyncRefreshRealtimeWithLock(any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 + HASH-ZSET ID 불일치 → 비동기 갱신 트리거")
    void shouldGetRealtimePosts_CacheHit_IdsMismatch_TriggerRefresh() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");
        List<PostSimpleDetail> cachedPosts = List.of(simpleDetail2, simpleDetail1);

        given(redisSimplePostAdapter.getAllCachedPostsList(RedisKey.REALTIME_SIMPLE_KEY))
                .willReturn(cachedPosts);
        given(redisRealTimePostAdapter.getRangePostId())
                .willReturn(List.of(3L, 2L));
        given(postUtil.paginate(cachedPosts, pageable))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: 기존 HASH 데이터 반환 + 비동기 갱신 트리거
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(realtimePostSync).asyncRefreshRealtimeWithLock(List.of(3L, 2L));
    }

    @Test
    @DisplayName("실시간 인기글 조회 - ZSET 조회 실패 → 비교 스킵, HASH 데이터 그대로 반환")
    void shouldGetRealtimePosts_ZSetFails_SkipComparison() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        List<PostSimpleDetail> cachedPosts = List.of(simpleDetail1);

        given(redisSimplePostAdapter.getAllCachedPostsList(RedisKey.REALTIME_SIMPLE_KEY))
                .willReturn(cachedPosts);
        given(redisRealTimePostAdapter.getRangePostId())
                .willThrow(new RuntimeException("ZSET 조회 실패"));
        given(postUtil.paginate(cachedPosts, pageable))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 1));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: HASH 데이터 그대로 반환, 비동기 갱신 미트리거
        assertThat(result.getContent()).hasSize(1);
        verify(realtimePostSync, never()).asyncRefreshRealtimeWithLock(any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 → 빈 페이지 반환 (스케줄러 갱신 예정)")
    void shouldGetRealtimePosts_CacheMiss_ReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);

        given(redisSimplePostAdapter.getAllCachedPostsList(RedisKey.REALTIME_SIMPLE_KEY)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: 빈 페이지 즉시 반환 (비동기 갱신 트리거 없음)
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
