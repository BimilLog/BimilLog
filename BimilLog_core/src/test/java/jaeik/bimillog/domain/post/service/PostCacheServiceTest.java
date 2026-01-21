package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.stream.Stream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostCacheService 테스트</h2>
 * <p>Hash 기반 캐시 조회 및 개수 비교/TTL 기반 PER 로직을 검증합니다.</p>
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
    private RedisTier2PostAdapter redisTier2PostAdapter;

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private PostCacheRefresh postCacheRefresh;

    @Mock
    private DbFallbackGateway dbFallbackGateway;

    @Mock
    private RealtimeScoreFallbackStore fallbackStore;

    @InjectMocks
    private PostCacheService postCacheService;

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트 (개수 일치, TTL 충분)")
    void shouldGetRealtimePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);
        cachedPosts.put(2L, simpleDetail2);

        given(redisRealTimePostAdapter.getRealtimePopularPostCount()).willReturn(2L);
        given(redisRealTimePostAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.REALTIME)).willReturn(false);
        given(redisSimplePostAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(simpleDetail1, simpleDetail2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisSimplePostAdapter).getAllCachedPosts(PostCacheFlag.REALTIME);
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 (개수 불일치 시 락 기반 전체 갱신 트리거)")
    void shouldGetRealtimePosts_CacheMiss_CountMismatch() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        // Tier1에 1개, Tier2에 2개 → 개수 불일치
        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);

        given(redisRealTimePostAdapter.getRealtimePopularPostCount()).willReturn(2L);
        given(redisRealTimePostAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(simpleDetail1));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh).asyncRefreshWithLock(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("실시간 인기글 조회 - PER 트리거 (개수 일치, TTL 임박)")
    void shouldGetRealtimePosts_PERTriggered() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);

        given(redisRealTimePostAdapter.getRealtimePopularPostCount()).willReturn(1L);
        given(redisRealTimePostAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.REALTIME)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.REALTIME)).willReturn(true); // TTL 임박
        given(redisSimplePostAdapter.toOrderedList(List.of(1L), cachedPosts)).willReturn(List.of(simpleDetail1));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh).asyncRefreshAllPosts(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 히트")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, weeklyPost1);
        cachedPosts.put(2L, weeklyPost2);

        given(redisTier2PostAdapter.getStoredPostIds(PostCacheFlag.WEEKLY)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.WEEKLY)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.WEEKLY)).willReturn(false);
        given(redisSimplePostAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(weeklyPost1, weeklyPost2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("주간 인기글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("주간 인기글 2");
        verify(postCacheRefresh, never()).asyncRefreshAllPosts(any());
    }

    @ParameterizedTest(name = "{0} - Tier2 비어있음")
    @EnumSource(value = PostCacheFlag.class, names = {"WEEKLY", "LEGEND", "NOTICE"})
    @DisplayName("Tier2 캐시 비어있음 - 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenTier2Empty(PostCacheFlag flag) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        given(redisTier2PostAdapter.getStoredPostIds(flag)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = switch (flag) {
            case WEEKLY -> postCacheService.getWeeklyPosts(pageable);
            case LEGEND -> postCacheService.getPopularPostLegend(pageable);
            case NOTICE -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

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

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, legendPost1);
        cachedPosts.put(2L, legendPost2);

        given(redisTier2PostAdapter.getStoredPostIds(PostCacheFlag.LEGEND)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.LEGEND)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.LEGEND)).willReturn(false);
        given(redisSimplePostAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(legendPost1, legendPost2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("레전드 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("레전드 게시글 2");
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, noticePost);

        given(redisTier2PostAdapter.getStoredPostIds(PostCacheFlag.NOTICE)).willReturn(List.of(1L));
        given(redisSimplePostAdapter.getAllCachedPosts(PostCacheFlag.NOTICE)).willReturn(cachedPosts);
        given(redisSimplePostAdapter.shouldRefreshHash(PostCacheFlag.NOTICE)).willReturn(false);
        given(redisSimplePostAdapter.toOrderedList(List.of(1L), cachedPosts)).willReturn(List.of(noticePost));

        // When
        Page<PostSimpleDetail> result = postCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("페이징 - offset이 전체 크기보다 큰 경우 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenOffsetExceedsTotalSize() {
        // Given
        Pageable pageable = PageRequest.of(10, 10); // offset = 100
        given(redisTier2PostAdapter.getStoredPostIds(PostCacheFlag.WEEKLY)).willReturn(List.of(1L, 2L)); // 총 2개

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
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

        given(redisTier2PostAdapter.getStoredPostIds(cacheFlag))
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
    @DisplayName("실시간 인기글 조회 - 총 개수 0이고 폴백 저장소 비어있으면 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenRealtimeCountIsZero() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        given(redisRealTimePostAdapter.getRealtimePopularPostCount()).willReturn(0L);
        given(fallbackStore.hasData()).willReturn(false);

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("실시간 인기글 조회 - postIds가 비어있으면 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenRealtimePostIdsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        given(redisRealTimePostAdapter.getRealtimePopularPostCount()).willReturn(5L);
        given(redisRealTimePostAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(5);
    }
}
