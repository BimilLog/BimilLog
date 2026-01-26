package jaeik.bimillog.domain.post.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostCacheService 테스트</h2>
 * <p>Hash 기반 캐시 조회 로직을 검증합니다.</p>
 * <p>모든 타입은 Hash 캐시에서 직접 조회하며, PER로 TTL 15초 미만 시 선제 갱신합니다.</p>
 * <p>캐시 미스 시 SET NX 락 기반으로 비동기 캐시 갱신을 트리거합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheService 테스트")
@Tag("unit")
class PostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private FeaturedPostRepository featuredPostRepository;

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

    private PostCacheService postCacheService;

    @BeforeEach
    void setUp() {
        // 기본: realtimeRedis 서킷 닫힘 (정상 상태)
        given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(realtimeRedisCircuitBreaker);
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        postCacheService = new PostCacheService(
                postQueryRepository,
                featuredPostRepository,
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
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.REALTIME);
        verify(redisSimplePostAdapter).shouldRefreshByPer(PostCacheFlag.REALTIME);
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
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
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: 캐시 데이터 반환 + PER 비동기 갱신 트리거 (락 없음)
        assertThat(result.getContent()).hasSize(1);
        verify(redisSimplePostAdapter).shouldRefreshByPer(PostCacheFlag.REALTIME);
        verify(redisSimplePostAdapter, never()).tryAcquireRefreshLock(any());
        verify(postCacheRefresh).asyncRefreshAllPosts(eq(PostCacheFlag.REALTIME), eq(List.of(1L, 2L)));
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 + 락 획득 성공 시 비동기 갱신 트리거")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CacheMiss_LockAcquired_RefreshTriggered() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME)).willReturn(List.of());
        given(redisSimplePostAdapter.tryAcquireRefreshLock(PostCacheFlag.REALTIME)).willReturn(true);
        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5)).willReturn(List.of(1L, 2L));

        given(dbFallbackGateway.execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(simpleDetail1)));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: DB 폴백 반환 + 비동기 갱신 트리거
        assertThat(result.getContent()).hasSize(1);
        verify(redisSimplePostAdapter).tryAcquireRefreshLock(PostCacheFlag.REALTIME);
        verify(realtimeAdapter).getRangePostId(PostCacheFlag.REALTIME, 0, 5);
        verify(postCacheRefresh).asyncRefreshAllPostsWithLock(eq(PostCacheFlag.REALTIME), eq(List.of(1L, 2L)));
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 + 락 획득 실패 시 갱신 스킵")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CacheMiss_LockNotAcquired_RefreshSkipped() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME)).willReturn(List.of());
        given(redisSimplePostAdapter.tryAcquireRefreshLock(PostCacheFlag.REALTIME)).willReturn(false);

        given(dbFallbackGateway.execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(simpleDetail1)));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: DB 폴백 반환, 비동기 갱신 스킵 (다른 스레드가 갱신 중)
        assertThat(result.getContent()).hasSize(1);
        verify(redisSimplePostAdapter).tryAcquireRefreshLock(PostCacheFlag.REALTIME);
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 + 실시간 인기글 ZSet도 비어있으면 락 해제")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CacheMiss_ZSetEmpty_ReleaseLock() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.REALTIME)).willReturn(List.of());
        given(redisSimplePostAdapter.tryAcquireRefreshLock(PostCacheFlag.REALTIME)).willReturn(true);
        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0, 5)).willReturn(List.of());

        given(dbFallbackGateway.execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: 실시간 인기글 ZSet 비어있으면 락 해제, 비동기 갱신 안 함
        assertThat(result.getContent()).isEmpty();
        verify(redisSimplePostAdapter).releaseRefreshLock(PostCacheFlag.REALTIME);
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 히트 + PER 미트리거")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.WEEKLY))
                .willReturn(List.of(weeklyPost2, weeklyPost1));
        given(redisSimplePostAdapter.shouldRefreshByPer(PostCacheFlag.WEEKLY)).willReturn(false);

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.WEEKLY);
        verify(redisSimplePostAdapter).shouldRefreshByPer(PostCacheFlag.WEEKLY);
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
    }

    @ParameterizedTest(name = "{0} - 캐시 비어있음 - 비동기 갱신 트리거 + DB 폴백")
    @EnumSource(value = PostCacheFlag.class, names = {"WEEKLY", "LEGEND", "NOTICE"})
    @DisplayName("캐시 비어있음 - 비동기 갱신 트리거 + DB 폴백")
    @SuppressWarnings("unchecked")
    void shouldTriggerAsyncRefreshAndFallbackToDb_WhenCacheEmpty(PostCacheFlag flag) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 게시글");

        given(redisSimplePostAdapter.getAllCachedPostsList(flag)).willReturn(List.of());
        given(redisSimplePostAdapter.tryAcquireRefreshLock(flag)).willReturn(true);

        FallbackType fallbackType = switch (flag) {
            case WEEKLY -> FallbackType.WEEKLY;
            case LEGEND -> FallbackType.LEGEND;
            case NOTICE -> FallbackType.NOTICE;
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        given(dbFallbackGateway.execute(eq(fallbackType), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // featured_post 테이블에서 조회하도록 변경
        given(featuredPostRepository.findPostIdsByType(flag)).willReturn(List.of(1L));
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(1L))).willReturn(List.of(post));

        // When
        Page<PostSimpleDetail> result = switch (flag) {
            case WEEKLY -> postCacheService.getWeeklyPosts(pageable);
            case LEGEND -> postCacheService.getPopularPostLegend(pageable);
            case NOTICE -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        // Then: 비동기 갱신 트리거 + DB 폴백 반환
        assertThat(result.getContent()).hasSize(1);
        verify(redisSimplePostAdapter).tryAcquireRefreshLock(flag);
        verify(postCacheRefresh).asyncRefreshAllPostsWithLock(eq(flag), eq(List.of()));
        verify(dbFallbackGateway).execute(eq(fallbackType), any(Pageable.class), any(Supplier.class));
    }

    @ParameterizedTest(name = "{0} - 캐시 비어있음 + 락 획득 실패 시 갱신 스킵")
    @EnumSource(value = PostCacheFlag.class, names = {"WEEKLY", "LEGEND", "NOTICE"})
    @DisplayName("캐시 비어있음 + 락 획득 실패 시 갱신 스킵")
    @SuppressWarnings("unchecked")
    void shouldSkipRefreshWhenLockNotAcquired_WhenCacheEmpty(PostCacheFlag flag) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 게시글");

        given(redisSimplePostAdapter.getAllCachedPostsList(flag)).willReturn(List.of());
        given(redisSimplePostAdapter.tryAcquireRefreshLock(flag)).willReturn(false);

        FallbackType fallbackType = switch (flag) {
            case WEEKLY -> FallbackType.WEEKLY;
            case LEGEND -> FallbackType.LEGEND;
            case NOTICE -> FallbackType.NOTICE;
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        given(dbFallbackGateway.execute(eq(fallbackType), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        given(featuredPostRepository.findPostIdsByType(flag)).willReturn(List.of(1L));
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(1L))).willReturn(List.of(post));

        // When
        Page<PostSimpleDetail> result = switch (flag) {
            case WEEKLY -> postCacheService.getWeeklyPosts(pageable);
            case LEGEND -> postCacheService.getPopularPostLegend(pageable);
            case NOTICE -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        // Then: 락 획득 실패 → 비동기 갱신 스킵, DB 폴백만 반환
        assertThat(result.getContent()).hasSize(1);
        verify(redisSimplePostAdapter).tryAcquireRefreshLock(flag);
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
        verify(dbFallbackGateway).execute(eq(fallbackType), any(Pageable.class), any(Supplier.class));
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 히트 + PER 미트리거")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.LEGEND))
                .willReturn(List.of(legendPost2, legendPost1));
        given(redisSimplePostAdapter.shouldRefreshByPer(PostCacheFlag.LEGEND)).willReturn(false);

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.LEGEND);
        verify(redisSimplePostAdapter).shouldRefreshByPer(PostCacheFlag.LEGEND);
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트 + PER 미트리거")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.NOTICE))
                .willReturn(List.of(noticePost));
        given(redisSimplePostAdapter.shouldRefreshByPer(PostCacheFlag.NOTICE)).willReturn(false);

        // When
        Page<PostSimpleDetail> result = postCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.NOTICE);
        verify(redisSimplePostAdapter).shouldRefreshByPer(PostCacheFlag.NOTICE);
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
    }

    @ParameterizedTest(name = "{0} - 캐시 히트 + PER 트리거 시 비동기 갱신 (락 없음)")
    @EnumSource(value = PostCacheFlag.class, names = {"WEEKLY", "LEGEND", "NOTICE"})
    @DisplayName("캐시 히트 + PER 트리거 시 비동기 갱신 (락 없음)")
    void shouldTriggerAsyncRefresh_WhenPerTriggered(PostCacheFlag flag) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, "캐시된 게시글");

        given(redisSimplePostAdapter.getAllCachedPostsList(flag)).willReturn(List.of(post));
        given(redisSimplePostAdapter.shouldRefreshByPer(flag)).willReturn(true);

        // When
        Page<PostSimpleDetail> result = switch (flag) {
            case WEEKLY -> postCacheService.getWeeklyPosts(pageable);
            case LEGEND -> postCacheService.getPopularPostLegend(pageable);
            case NOTICE -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        // Then: 캐시 데이터 반환 + PER 비동기 갱신 트리거 (락 없음)
        assertThat(result.getContent()).hasSize(1);
        verify(redisSimplePostAdapter).shouldRefreshByPer(flag);
        verify(redisSimplePostAdapter, never()).tryAcquireRefreshLock(any());
        verify(postCacheRefresh).asyncRefreshAllPosts(eq(flag), eq(List.of()));
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
    }

    @ParameterizedTest(name = "{0} - Redis 장애 시 DB fallback")
    @MethodSource("provideRedisFallbackScenarios")
    @DisplayName("Redis 장애 시 DB fallback")
    @SuppressWarnings("unchecked")
    void shouldFallbackToDb_WhenRedisFails(PostCacheFlag cacheFlag, FallbackType fallbackType, String expectedTitle) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, expectedTitle);

        given(redisSimplePostAdapter.getAllCachedPostsList(cacheFlag))
                .willThrow(new RuntimeException("Redis connection failed"));

        given(dbFallbackGateway.execute(eq(fallbackType), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // featured_post 테이블에서 조회하도록 변경
        given(featuredPostRepository.findPostIdsByType(cacheFlag)).willReturn(List.of(1L));
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(1L))).willReturn(List.of(post));

        // When
        Page<PostSimpleDetail> result = switch (cacheFlag) {
            case WEEKLY -> postCacheService.getWeeklyPosts(pageable);
            case LEGEND -> postCacheService.getPopularPostLegend(pageable);
            case NOTICE -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + cacheFlag);
        };

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(expectedTitle);
        verify(dbFallbackGateway).execute(eq(fallbackType), any(Pageable.class), any(Supplier.class));
    }

    static Stream<Arguments> provideRedisFallbackScenarios() {
        return Stream.of(
                Arguments.of(PostCacheFlag.WEEKLY, FallbackType.WEEKLY, "주간 인기글"),
                Arguments.of(PostCacheFlag.LEGEND, FallbackType.LEGEND, "레전드 게시글"),
                Arguments.of(PostCacheFlag.NOTICE, FallbackType.NOTICE, "공지사항")
        );
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
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(simpleDetail1)));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

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
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: Redis 접근 없이 Caffeine → DB 경로 사용
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("인기글 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("인기글 1");

        verify(redisSimplePostAdapter, never()).getAllCachedPostsList(any());
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
        verify(dbFallbackGateway).executeList(eq(FallbackType.REALTIME), eq(List.of(2L, 1L)), any(Supplier.class));
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 서킷 OPEN + Caffeine 비어있음 → 일반 DB 폴백")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CircuitOpen_CaffeineEmpty_FallbackToDb() {
        // Given: 서킷 OPEN + Caffeine 비어있음
        given(realtimeRedisCircuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);

        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");

        given(realtimeScoreFallbackStore.getTopPostIds(0, 5)).willReturn(List.of());
        given(dbFallbackGateway.execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findRecentPopularPosts(pageable))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(post1)));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: Redis 접근 없이 일반 DB 폴백
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("DB 폴백 글");

        verify(redisSimplePostAdapter, never()).getAllCachedPostsList(any());
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
        verify(dbFallbackGateway).execute(eq(FallbackType.REALTIME), any(Pageable.class), any(Supplier.class));
    }
}
