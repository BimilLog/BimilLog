package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.FeaturedPostRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
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
 * <h2>FeaturedPostCacheService 테스트</h2>
 * <p>주간/레전드/공지 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>Hash 캐시 조회, 캐시 미스 시 SET NX 락 기반 비동기 갱신 + DB 폴백을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeaturedPostCacheService 테스트")
@Tag("unit")
class FeaturedPostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private FeaturedPostRepository featuredPostRepository;

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Mock
    private PostCacheRefresh postCacheRefresh;

    @Mock
    private DbFallbackGateway dbFallbackGateway;

    private FeaturedPostCacheService featuredPostCacheService;

    @BeforeEach
    void setUp() {
        featuredPostCacheService = new FeaturedPostCacheService(
                postQueryRepository,
                featuredPostRepository,
                redisSimplePostAdapter,
                postCacheRefresh,
                dbFallbackGateway
        );
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 히트")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.WEEKLY))
                .willReturn(List.of(weeklyPost2, weeklyPost1));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.WEEKLY);
        verify(postCacheRefresh, never()).asyncRefreshFeaturedWithLock(any());
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 히트")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.LEGEND))
                .willReturn(List.of(legendPost2, legendPost1));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.LEGEND);
        verify(postCacheRefresh, never()).asyncRefreshFeaturedWithLock(any());
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.NOTICE))
                .willReturn(List.of(noticePost));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.NOTICE);
        verify(postCacheRefresh, never()).asyncRefreshFeaturedWithLock(any());
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
            case WEEKLY -> featuredPostCacheService.getWeeklyPosts(pageable);
            case LEGEND -> featuredPostCacheService.getPopularPostLegend(pageable);
            case NOTICE -> featuredPostCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        // Then: 비동기 갱신 트리거 + DB 폴백 반환 (락은 비동기 내부에서 처리)
        assertThat(result.getContent()).hasSize(1);
        verify(postCacheRefresh).asyncRefreshFeaturedWithLock(flag);
        verify(dbFallbackGateway).execute(eq(fallbackType), any(Pageable.class), any(Supplier.class));
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

        given(featuredPostRepository.findPostIdsByType(cacheFlag)).willReturn(List.of(1L));
        given(postQueryRepository.findPostSimpleDetailsByIds(List.of(1L))).willReturn(List.of(post));

        // When
        Page<PostSimpleDetail> result = switch (cacheFlag) {
            case WEEKLY -> featuredPostCacheService.getWeeklyPosts(pageable);
            case LEGEND -> featuredPostCacheService.getPopularPostLegend(pageable);
            case NOTICE -> featuredPostCacheService.getNoticePosts(pageable);
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
}
