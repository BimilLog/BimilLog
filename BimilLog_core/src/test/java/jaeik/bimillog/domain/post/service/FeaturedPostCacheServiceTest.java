package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
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

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>FeaturedPostCacheService 테스트</h2>
 * <p>주간/레전드/공지 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>Hash 캐시 조회, 캐시 미스 시 빈 페이지 반환, 예외 시 DB 폴백 경로를 검증합니다.</p>
 * <p>DB 폴백은 Post.featuredType 기반 쿼리를 사용합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeaturedPostCacheService 테스트")
@Tag("unit")
class FeaturedPostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Mock
    private PostUtil postUtil;

    private FeaturedPostCacheService featuredPostCacheService;

    @BeforeEach
    void setUp() {
        featuredPostCacheService = new FeaturedPostCacheService(
                postQueryRepository,
                redisSimplePostAdapter,
                postUtil
        );
    }

    @Test
    @DisplayName("주간 인기글 조회 - 캐시 히트")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");
        List<PostSimpleDetail> cachedPosts = List.of(weeklyPost2, weeklyPost1);

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.WEEKLY))
                .willReturn(cachedPosts);
        given(postUtil.paginate(cachedPosts, pageable))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 히트")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        List<PostSimpleDetail> cachedPosts = List.of(legendPost2, legendPost1);

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.LEGEND))
                .willReturn(cachedPosts);
        given(postUtil.paginate(cachedPosts, pageable))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.LEGEND);
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<PostSimpleDetail> cachedPosts = List.of(noticePost);

        given(redisSimplePostAdapter.getAllCachedPostsList(PostCacheFlag.NOTICE))
                .willReturn(cachedPosts);
        given(postUtil.paginate(cachedPosts, pageable))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 1));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        verify(redisSimplePostAdapter).getAllCachedPostsList(PostCacheFlag.NOTICE);
    }

    @ParameterizedTest(name = "{0} - 캐시 비어있음 - 빈 페이지 반환 (스케줄러 갱신 예정)")
    @EnumSource(value = PostCacheFlag.class, names = {"WEEKLY", "LEGEND", "NOTICE"})
    @DisplayName("캐시 비어있음 - 빈 페이지 반환 (스케줄러 갱신 예정)")
    void shouldReturnEmptyPage_WhenCacheEmpty(PostCacheFlag flag) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        given(redisSimplePostAdapter.getAllCachedPostsList(flag)).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = switch (flag) {
            case WEEKLY -> featuredPostCacheService.getWeeklyPosts(pageable);
            case LEGEND -> featuredPostCacheService.getPopularPostLegend(pageable);
            case NOTICE -> featuredPostCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unsupported flag: " + flag);
        };

        // Then: 빈 페이지 즉시 반환 (비동기 갱신 트리거 없음)
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @ParameterizedTest(name = "{0} - Redis 장애 시 DB fallback (featuredType 기반)")
    @MethodSource("provideRedisFallbackScenarios")
    @DisplayName("Redis 장애 시 DB fallback (featuredType 기반)")
    void shouldFallbackToDb_WhenRedisFails(PostCacheFlag cacheFlag, String expectedTitle) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, expectedTitle);

        given(redisSimplePostAdapter.getAllCachedPostsList(cacheFlag))
                .willThrow(new RuntimeException("Redis connection failed"));

        given(postQueryRepository.findPostsByFeaturedType(eq(cacheFlag), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post), pageable, 1));

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
        verify(postQueryRepository).findPostsByFeaturedType(eq(cacheFlag), any(Pageable.class));
    }

    static Stream<Arguments> provideRedisFallbackScenarios() {
        return Stream.of(
                Arguments.of(PostCacheFlag.WEEKLY, "주간 인기글"),
                Arguments.of(PostCacheFlag.LEGEND, "레전드 게시글"),
                Arguments.of(PostCacheFlag.NOTICE, "공지사항")
        );
    }
}
