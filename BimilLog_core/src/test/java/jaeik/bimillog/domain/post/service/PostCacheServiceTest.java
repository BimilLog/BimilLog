package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostAdapter;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.stream.Stream;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>PostCacheService 테스트</h2>
 * <p>MGET 기반 개별 캐시 조회 및 Redis TTL 기반 PER 로직을 검증합니다.</p>
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

    @InjectMocks
    private PostCacheService postCacheService;

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 히트")
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
        given(redisSimplePostAdapter.getCachedPosts(PostCacheFlag.REALTIME, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisSimplePostAdapter.filterRefreshNeeded(PostCacheFlag.REALTIME, List.of(1L, 2L))).willReturn(List.of());
        given(redisSimplePostAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of());
        given(redisSimplePostAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(simpleDetail1, simpleDetail2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("실시간 인기글 2");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisSimplePostAdapter).getCachedPosts(PostCacheFlag.REALTIME, List.of(1L, 2L));
        verify(postCacheRefresh, never()).asyncRefreshPosts(any(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 시 DB 조회")
    void shouldGetRealtimePosts_CacheMiss() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "캐시된 게시글");
        PostDetail realtimePost1 = createPostDetail(1L, "DB에서 조회된 게시글");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(2L, simpleDetail2);

        given(redisRealTimePostAdapter.getRealtimePopularPostCount()).willReturn(2L);
        given(redisRealTimePostAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getCachedPosts(PostCacheFlag.REALTIME, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisSimplePostAdapter.filterRefreshNeeded(PostCacheFlag.REALTIME, List.of(2L))).willReturn(List.of());
        given(redisSimplePostAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of(1L));
        given(postQueryRepository.findPostDetail(1L, null)).willReturn(Optional.of(realtimePost1));

        // toOrderedList는 cachedPosts가 업데이트된 후 호출됨
        PostSimpleDetail simpleDetail1 = realtimePost1.toSimpleDetail();
        given(redisSimplePostAdapter.toOrderedList(eq(List.of(1L, 2L)), any())).willReturn(List.of(simpleDetail1, simpleDetail2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(postQueryRepository).findPostDetail(1L, null);
        verify(redisSimplePostAdapter).cachePost(eq(PostCacheFlag.REALTIME), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - PER 트리거")
    void shouldGetRealtimePosts_PERTriggered() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, simpleDetail1);

        given(redisRealTimePostAdapter.getRealtimePopularPostCount()).willReturn(1L);
        given(redisRealTimePostAdapter.getRealtimePopularPostIds(0L, 5)).willReturn(List.of(1L));
        given(redisSimplePostAdapter.getCachedPosts(PostCacheFlag.REALTIME, List.of(1L))).willReturn(cachedPosts);
        given(redisSimplePostAdapter.filterRefreshNeeded(PostCacheFlag.REALTIME, List.of(1L))).willReturn(List.of(1L)); // PER 대상
        given(redisSimplePostAdapter.filterMissedIds(List.of(1L), cachedPosts)).willReturn(List.of());
        given(redisSimplePostAdapter.toOrderedList(List.of(1L), cachedPosts)).willReturn(List.of(simpleDetail1));

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

        Map<Long, PostSimpleDetail> cachedPosts = new HashMap<>();
        cachedPosts.put(1L, weeklyPost1);
        cachedPosts.put(2L, weeklyPost2);

        given(redisTier2PostAdapter.getStoredPostIds(PostCacheFlag.WEEKLY)).willReturn(List.of(1L, 2L));
        given(redisSimplePostAdapter.getCachedPosts(PostCacheFlag.WEEKLY, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisSimplePostAdapter.filterRefreshNeeded(PostCacheFlag.WEEKLY, List.of(1L, 2L))).willReturn(List.of());
        given(redisSimplePostAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of());
        given(redisSimplePostAdapter.toOrderedList(List.of(1L, 2L), cachedPosts)).willReturn(List.of(weeklyPost1, weeklyPost2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("주간 인기글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("주간 인기글 2");
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
        given(redisSimplePostAdapter.getCachedPosts(PostCacheFlag.LEGEND, List.of(1L, 2L))).willReturn(cachedPosts);
        given(redisSimplePostAdapter.filterRefreshNeeded(PostCacheFlag.LEGEND, List.of(1L, 2L))).willReturn(List.of());
        given(redisSimplePostAdapter.filterMissedIds(List.of(1L, 2L), cachedPosts)).willReturn(List.of());
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
        given(redisSimplePostAdapter.getCachedPosts(PostCacheFlag.NOTICE, List.of(1L))).willReturn(cachedPosts);
        given(redisSimplePostAdapter.filterRefreshNeeded(PostCacheFlag.NOTICE, List.of(1L))).willReturn(List.of());
        given(redisSimplePostAdapter.filterMissedIds(List.of(1L), cachedPosts)).willReturn(List.of());
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

        given(redisTier2PostAdapter.getStoredPostIds(cacheFlag))
                .willThrow(new RuntimeException("Redis connection failed"));

        given(dbFallbackGateway.execute(eq(fallbackType), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // 각 캐시 플래그에 따른 repository 메서드 Mock 설정
        switch (cacheFlag) {
            case WEEKLY -> given(postQueryRepository.findWeeklyPopularPosts()).willReturn(List.of(post));
            case LEGEND -> given(postQueryRepository.findLegendaryPosts()).willReturn(List.of(post));
            case NOTICE -> given(postQueryRepository.findNoticePosts()).willReturn(List.of(post));
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
    @DisplayName("Redis 장애 시 DB fallback - 페이징 처리")
    @SuppressWarnings("unchecked")
    void shouldGetWeeklyPosts_RedisFallbackToDb_WithPaging() {
        // Given
        Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 2개씩
        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "게시글 1");
        PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "게시글 2");
        PostSimpleDetail post3 = PostTestDataBuilder.createPostSearchResult(3L, "게시글 3");
        PostSimpleDetail post4 = PostTestDataBuilder.createPostSearchResult(4L, "게시글 4");

        given(redisTier2PostAdapter.getStoredPostIds(PostCacheFlag.WEEKLY))
                .willThrow(new RuntimeException("Redis connection failed"));

        // DbFallbackGateway가 Supplier를 실행하도록 Mock 설정
        given(dbFallbackGateway.execute(eq(FallbackType.WEEKLY), any(Pageable.class), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Page<PostSimpleDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        given(postQueryRepository.findWeeklyPopularPosts()).willReturn(List.of(post1, post2, post3, post4));

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("게시글 3");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("게시글 4");
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
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
