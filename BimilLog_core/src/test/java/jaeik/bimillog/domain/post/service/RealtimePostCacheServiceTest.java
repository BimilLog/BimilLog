package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>RealtimePostCacheService 테스트</h2>
 * <p>실시간 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>Hash 캐시 조회, 캐시 미스 시 빈 페이지 반환, 예외 시 DB 서킷 경로를 검증합니다.</p>
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
    private PostCacheRefresh postCacheRefresh;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CircuitBreaker realtimeRedisCircuitBreaker;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    private RealtimePostCacheService realtimePostCacheService;

    @BeforeEach
    void setUp() {
        // 기본: realtimeRedis 서킷 닫힘 (정상 상태)
        given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(realtimeRedisCircuitBreaker);
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        // 기본: ZSET 조회 시 빈 리스트 반환 (비교 로직에서 사용)
        given(redisRealTimePostAdapter.getRangePostId(any(), anyLong(), anyLong())).willReturn(List.of());

        realtimePostCacheService = new RealtimePostCacheService(
                postQueryRepository,
                redisSimplePostAdapter,
                redisRealTimePostAdapter,
                postCacheRefresh,
                circuitBreakerRegistry,
                realtimeScoreFallbackStore
        );
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 + HASH-ZSET ID 일치 → 비동기 갱신 없음")
    void shouldGetRealtimePosts_CacheHit_IdsMatch() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME))
                .willReturn(List.of(simpleDetail2, simpleDetail1));
        given(redisRealTimePostAdapter.getRangePostId(any(), anyLong(), anyLong()))
                .willReturn(List.of(2L, 1L));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.REALTIME);
        verify(postCacheRefresh, never()).asyncRefreshRealtimeWithLock();
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 + HASH-ZSET ID 불일치 → 비동기 갱신 트리거")
    void shouldGetRealtimePosts_CacheHit_IdsMismatch_TriggerRefresh() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME))
                .willReturn(List.of(simpleDetail2, simpleDetail1));
        // ZSET에는 다른 ID 세트 반환
        given(redisRealTimePostAdapter.getRangePostId(any(), anyLong(), anyLong()))
                .willReturn(List.of(3L, 2L));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: 기존 HASH 데이터 반환 + 비동기 갱신 트리거
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(postCacheRefresh).asyncRefreshRealtimeWithLock();
    }

    @Test
    @DisplayName("실시간 인기글 조회 - ZSET 조회 실패 → 비교 스킵, HASH 데이터 그대로 반환")
    void shouldGetRealtimePosts_ZSetFails_SkipComparison() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME))
                .willReturn(List.of(simpleDetail1));
        given(redisRealTimePostAdapter.getRangePostId(any(), anyLong(), anyLong()))
                .willThrow(new RuntimeException("ZSET 조회 실패"));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: HASH 데이터 그대로 반환, 비동기 갱신 미트리거
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh, never()).asyncRefreshRealtimeWithLock();
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 → 빈 페이지 반환 (스케줄러 갱신 예정)")
    void shouldGetRealtimePosts_CacheMiss_ReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: 빈 페이지 즉시 반환 (비동기 갱신 트리거 없음)
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("실시간 인기글 조회 - Redis 장애 시 DB fallback")
    void shouldGetRealtimePosts_RedisFails_FallbackToDb() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME))
                .willThrow(new RuntimeException("Redis connection failed"));

        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new PageImpl<>(List.of(simpleDetail1)));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(postQueryRepository).findRecentPopularPosts(pageable);
    }

    // ========== 서킷 OPEN 테스트 ==========

    @Test
    @DisplayName("실시간 인기글 조회 - 서킷 OPEN + Caffeine 데이터 있음 → 빈 페이지 반환")
    void shouldGetRealtimePosts_CircuitOpen_CaffeineHasData_ReturnEmptyPage() {
        // Given: 서킷 OPEN
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        Pageable pageable = PageRequest.of(0, 5);

        given(realtimeScoreFallbackStore.getTopPostIds(0, 5)).willReturn(List.of(2L, 1L));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: Redis 접근 없이 빈 페이지 반환
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(redisSimplePostAdapter, never()).getAllCachedPostsList(any());
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 서킷 OPEN + Caffeine 비어있음 → 빈 페이지 반환")
    void shouldGetRealtimePosts_CircuitOpen_CaffeineEmpty_ReturnEmptyPage() {
        // Given: 서킷 OPEN + Caffeine 비어있음
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        Pageable pageable = PageRequest.of(0, 5);

        given(realtimeScoreFallbackStore.getTopPostIds(0, 5)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: Redis 접근 없이 빈 페이지 반환
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(redisSimplePostAdapter, never()).getAllCachedPostsList(any());
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 서킷 OPEN + Caffeine 예외 → DB 폴백")
    void shouldGetRealtimePosts_CircuitOpen_CaffeineFails_FallbackToDb() {
        // Given: 서킷 OPEN + Caffeine 예외
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");

        given(realtimeScoreFallbackStore.getTopPostIds(0, 5))
                .willThrow(new RuntimeException("Caffeine 장애"));

        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new PageImpl<>(List.of(post1)));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: Caffeine 예외 시 DB 폴백
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("DB 폴백 글");

        verify(redisSimplePostAdapter, never()).getAllCachedPostsList(any());
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
        verify(postQueryRepository).findRecentPopularPosts(pageable);
    }
}
