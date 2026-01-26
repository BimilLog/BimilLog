package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>RealtimePostCacheService 테스트</h2>
 * <p>실시간 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>Hash 캐시 조회, PER 선제 갱신, 캐시 미스 시 ZSet → DB 폴백, 서킷 OPEN 시 Caffeine 폴백을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealtimePostCacheService 테스트")
@Tag("unit")
class RealtimePostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Mock
    private RedisRealTimePostAdapter realtimeAdapter;

    @Mock
    private PostCacheRefresh postCacheRefresh;

    @Mock
    private DbFallbackGateway dbFallbackGateway;

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

        realtimePostCacheService = new RealtimePostCacheService(
                postQueryRepository,
                redisSimplePostAdapter,
                realtimeAdapter,
                postCacheRefresh,
                dbFallbackGateway,
                circuitBreakerRegistry,
                realtimeScoreFallbackStore
        );
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 + PER 미트리거")
    void shouldGetRealtimePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME))
                .willReturn(List.of(simpleDetail2, simpleDetail1));
        given(redisSimplePostAdapter.shouldRefreshByPer(PostCacheFlag.REALTIME)).willReturn(false);

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.REALTIME);
        verify(redisSimplePostAdapter).shouldRefreshByPer(PostCacheFlag.REALTIME);
        verify(postCacheRefresh, never()).asyncRefreshRealtimeWithLock(any());
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 + PER 트리거 시 비동기 갱신 (락 없음)")
    void shouldGetRealtimePosts_CacheHit_PerTriggered() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME))
                .willReturn(List.of(simpleDetail1));
        given(redisSimplePostAdapter.shouldRefreshByPer(PostCacheFlag.REALTIME)).willReturn(true);
        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5)).willReturn(List.of(1L, 2L));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: 캐시 데이터 반환 + PER 비동기 갱신 트리거 (락 없음)
        assertThat(result.getContent()).hasSize(1);
        verify(redisSimplePostAdapter).shouldRefreshByPer(PostCacheFlag.REALTIME);
        verify(redisSimplePostAdapter, never()).tryAcquireRefreshLock(any());
        verify(postCacheRefresh).asyncRefreshAllPosts(eq(PostCacheFlag.REALTIME), eq(List.of(1L, 2L)));
        verify(postCacheRefresh, never()).asyncRefreshRealtimeWithLock(any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 + ZSet 데이터 있음 → DB 폴백 반환 + 비동기 갱신")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CacheMiss_ZSetHasData_FallbackToDb() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "인기글 1");
        PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "인기글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME)).willReturn(List.of());
        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5)).willReturn(List.of(2L, 1L));
        given(dbFallbackGateway.executeList(eq(FallbackType.REALTIME), eq(List.of(2L, 1L)), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<List<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(2L, 1L)))
                .willReturn(List.of(post2, post1));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: ZSet ID로 DB 조회 반환 + 조회된 데이터로 비동기 캐시 갱신 트리거
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("인기글 2");
        verify(realtimeAdapter).getRangePostId(PostCacheFlag.REALTIME, 0, 5);
        verify(dbFallbackGateway).executeList(eq(FallbackType.REALTIME), eq(List.of(2L, 1L)), any(Supplier.class));
        verify(postCacheRefresh).asyncRefreshRealtimeWithLock(List.of(post2, post1));
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 + ZSet 비어있음 → 빈 페이지 반환, 비동기 없음")
    void shouldGetRealtimePosts_CacheMiss_ZSetEmpty_ReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME)).willReturn(List.of());
        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: 빈 페이지 반환, 비동기 갱신 트리거 없음
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(realtimeAdapter).getRangePostId(PostCacheFlag.REALTIME, 0, 5);
        verify(postCacheRefresh, never()).asyncRefreshRealtimeWithLock(any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - Redis 장애 시 DB fallback")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_RedisFails_FallbackToDb() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME))
                .willThrow(new RuntimeException("Redis connection failed"));

        given(dbFallbackGateway.execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new PageImpl<>(List.of(simpleDetail1)));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(dbFallbackGateway).execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class));
    }

    // ========== 서킷 OPEN 테스트 ==========

    @Test
    @DisplayName("실시간 인기글 조회 - 서킷 OPEN + Caffeine 데이터 있음 → Caffeine → DB")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CircuitOpen_CaffeineHasData() {
        // Given: 서킷 OPEN
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "인기글 1");
        PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "인기글 2");

        given(realtimeScoreFallbackStore.getTopPostIds(0, 5)).willReturn(List.of(2L, 1L));
        given(dbFallbackGateway.executeList(eq(FallbackType.REALTIME), eq(List.of(2L, 1L)), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<List<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(2L, 1L)))
                .willReturn(List.of(post2, post1));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: Redis 접근 없이 Caffeine → DB 경로 사용
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("인기글 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("인기글 1");

        verify(redisSimplePostAdapter, never()).getAllCachedPostsList(any());
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
        verify(dbFallbackGateway).executeList(eq(FallbackType.REALTIME), eq(List.of(2L, 1L)), any(Supplier.class));
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
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CircuitOpen_CaffeineFails_FallbackToDb() {
        // Given: 서킷 OPEN + Caffeine 예외
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");

        given(realtimeScoreFallbackStore.getTopPostIds(0, 5))
                .willThrow(new RuntimeException("Caffeine 장애"));

        given(dbFallbackGateway.execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new PageImpl<>(List.of(post1)));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then: Caffeine 예외 시 DB 폴백
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("DB 폴백 글");

        verify(redisSimplePostAdapter, never()).getAllCachedPostsList(any());
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
        verify(dbFallbackGateway).execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class));
    }
}
