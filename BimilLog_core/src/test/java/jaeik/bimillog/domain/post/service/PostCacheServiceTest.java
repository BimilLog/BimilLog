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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>주간/레전드/공지는 Tier1 직접 조회, 실시간만 Score 저장소와 함께 사용합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheService 테스트")
@Tag("unit")
class PostCacheServiceTest {

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

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    private PostCacheService postCacheService;

    @BeforeEach
    void setUp() {
        postCacheService = new PostCacheService(
                postQueryRepository,
                redisSimplePostAdapter,
                realtimeAdapter,
                postCacheRefresh,
                dbFallbackGateway,
                circuitBreakerRegistry
        );
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 (ID 일치, TTL 충분)")
    void shouldGetRealtimePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);
        cachedPosts.put(2L, simpleDetail2);

        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0L, 5)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.isCacheIdsMatch(List.of(1L, 2L), cachedPosts.keySet())).willReturn(true);
        given(redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.REALTIME)).willReturn(false);
        given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(circuitBreaker);
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisSimplePostAdapter).getAllCachedPosts(PostCacheFlag.REALTIME);
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - ID 불일치 + 락 획득 성공 시 갱신 트리거")
    void shouldGetRealtimePosts_IdMismatch_LockAcquired_RefreshTriggered() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        // Tier1에 1개, postIds에 2개 → ID 불일치
        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);

        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0L, 5)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.isCacheIdsMatch(List.of(1L, 2L), cachedPosts.keySet())).willReturn(false);
        given(redisSimplePostAdapter.tryAcquireRefreshLock()).willReturn(true);
        given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(circuitBreaker);
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: 과거 캐시 반환 + 락 획득 후 갱신
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh).asyncRefreshAllPostsWithLock(eq(PostCacheFlag.REALTIME), any());
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - ID 불일치 + 락 획득 실패 시 갱신 스킵")
    void shouldGetRealtimePosts_IdMismatch_LockNotAcquired_RefreshSkipped() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        // Tier1에 1개, postIds에 2개 → ID 불일치
        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);

        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0L, 5)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.isCacheIdsMatch(List.of(1L, 2L), cachedPosts.keySet())).willReturn(false);
        given(redisSimplePostAdapter.tryAcquireRefreshLock()).willReturn(false);
        given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(circuitBreaker);
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then: 과거 캐시 반환, 갱신 스킵 (다른 스레드가 갱신 중)
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - PER 트리거 (ID 일치, TTL 임박)")
    void shouldGetRealtimePosts_PERTriggered() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);

        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0L, 5)).willReturn(List.of(1L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.isCacheIdsMatch(List.of(1L), cachedPosts.keySet())).willReturn(true);
        given(redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.REALTIME)).willReturn(true); // TTL 임박
        given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(circuitBreaker);
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.CLOSED);

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh).asyncRefreshAllPosts(eq(PostCacheFlag.REALTIME), any());
        verify(postCacheRefresh, never()).asyncRefreshAllPostsWithLock(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 서킷 OPEN 시 DB 조회")
    @SuppressWarnings("unchecked")
    void shouldGetRealtimePosts_CircuitOpen_FallbackToDb() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0L, 5)).willReturn(List.of(1L));
                given(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).willReturn(circuitBreaker);
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(1L))).willReturn(List.of(simpleDetail1));

        // DbFallbackGateway가 Supplier를 실행하도록 Mock 설정
        given(dbFallbackGateway.executeList(eq(FallbackType.REALTIME), eq(List.of(1L)), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<List<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 1");
        verify(redisSimplePostAdapter, never()).getAllCachedPosts(any());
        verify(dbFallbackGateway).executeList(eq(FallbackType.REALTIME), eq(List.of(1L)), any(Supplier.class));
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 히트 (Tier1 직접 조회)")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");

        // Tier1에서 직접 List로 조회 (Tier2 제거됨)
        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.WEEKLY))
                .willReturn(List.of(weeklyPost2, weeklyPost1)); // ID 역순 정렬

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.WEEKLY);
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any(), any());
    }

    @ParameterizedTest(name = "{0} - 캐시 비어있음 - DB 폴백")
    @EnumSource(value = PostCacheFlag.class, names = {"WEEKLY", "LEGEND", "NOTICE"})
    @DisplayName("캐시 비어있음 - DB 폴백 후 스케줄러 대기")
    @SuppressWarnings("unchecked")
    void shouldFallbackToDb_WhenCacheEmpty(PostCacheFlag flag) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 게시글");
        Page<PostSimpleDetail> fallbackPage = new PageImpl<>(List.of(post), pageable, 1);

        given(redisSimplePostAdapter.getAllCachedPostsList(flag)).willReturn(List.of());

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

        switch (flag) {
            case WEEKLY -> given(postQueryRepository.findWeeklyPopularPosts(pageable)).willReturn(fallbackPage);
            case LEGEND -> given(postQueryRepository.findLegendaryPosts(pageable)).willReturn(fallbackPage);
            case NOTICE -> given(postQueryRepository.findNoticePosts(pageable)).willReturn(fallbackPage);
        }

        // When
        Page<PostSimpleDetail> result = switch (flag) {
            case WEEKLY -> postCacheService.getWeeklyPosts(pageable);
            case LEGEND -> postCacheService.getPopularPostLegend(pageable);
            case NOTICE -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(dbFallbackGateway).execute(eq(fallbackType), any(Pageable.class), any(Supplier.class));
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 (Tier1 직접 조회)")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.LEGEND))
                .willReturn(List.of(legendPost2, legendPost1));

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.LEGEND);
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트 (Tier1 직접 조회)")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.NOTICE))
                .willReturn(List.of(noticePost));

        // When
        Page<PostSimpleDetail> result = postCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.NOTICE);
    }

    @ParameterizedTest(name = "{0} - Redis 장애 시 DB fallback")
    @MethodSource("provideRedisFallbackScenarios")
    @DisplayName("Redis 장애 시 DB fallback")
    @SuppressWarnings("unchecked")
    void shouldFallbackToDb_WhenRedisFails(PostCacheFlag cacheFlag, FallbackType fallbackType, String expectedTitle) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, expectedTitle);
        Page<PostSimpleDetail> fallbackPage = new PageImpl<>(List.of(post), pageable, 1);

        given(redisSimplePostAdapter.getAllCachedPostsList(cacheFlag))
                .willThrow(new RuntimeException("Redis connection failed"));

        given(dbFallbackGateway.execute(eq(fallbackType), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // 각 캐시 플래그에 따른 repository 메서드 Mock 설정 (Page 반환)
        switch (cacheFlag) {
            case WEEKLY -> given(postQueryRepository.findWeeklyPopularPosts(pageable)).willReturn(fallbackPage);
            case LEGEND -> given(postQueryRepository.findLegendaryPosts(pageable)).willReturn(fallbackPage);
            case NOTICE -> given(postQueryRepository.findNoticePosts(pageable)).willReturn(fallbackPage);
            default -> throw new IllegalArgumentException("Unsupported flag: " + cacheFlag);
        }

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
    @DisplayName("실시간 인기글 조회 - postIds가 비어있으면 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenRealtimePostIdsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        given(realtimeAdapter.getRangePostId(PostCacheFlag.REALTIME, 0L, 5)).willReturn(List.of());
        
        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
