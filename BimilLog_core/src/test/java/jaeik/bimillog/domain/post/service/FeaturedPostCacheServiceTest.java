package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FeaturedPostCacheService 테스트</h2>
 * <p>주간/레전드/공지 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>SET 인덱스 → 글 단위 Hash pipeline 조회, 캐시 미스 시 빈 페이지 반환, 예외 시 DB 폴백 경로를 검증합니다.</p>
 * <p>DB 폴백은 Post.featuredType 기반 쿼리를 사용합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeaturedPostCacheService 테스트")
@Tag("unit")
class FeaturedPostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisPostHashAdapter redisPostHashAdapter;

    @Mock
    private RedisPostIndexAdapter redisPostIndexAdapter;

    @Mock
    private PostUtil postUtil;

    private FeaturedPostCacheService featuredPostCacheService;

    @BeforeEach
    void setUp() {
        featuredPostCacheService = new FeaturedPostCacheService(
                postQueryRepository,
                redisPostHashAdapter,
                redisPostIndexAdapter,
                postUtil
        );
    }

    @Test
    @DisplayName("주간 인기글 조회 - SET 인덱스 + 글 단위 Hash 캐시 히트")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");
        List<PostSimpleDetail> cachedPosts = List.of(weeklyPost2, weeklyPost1);

        given(redisPostIndexAdapter.getIndexMembers(RedisKey.POST_WEEKLY_IDS_KEY))
                .willReturn(Set.of(1L, 2L));
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(cachedPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisPostIndexAdapter).getIndexMembers(RedisKey.POST_WEEKLY_IDS_KEY);
        verify(redisPostHashAdapter).getPostHashes(anyCollection());
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 히트")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        List<PostSimpleDetail> cachedPosts = List.of(legendPost2, legendPost1);

        given(redisPostIndexAdapter.getIndexMembers(RedisKey.POST_LEGEND_IDS_KEY))
                .willReturn(Set.of(1L, 2L));
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(cachedPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisPostIndexAdapter).getIndexMembers(RedisKey.POST_LEGEND_IDS_KEY);
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<PostSimpleDetail> cachedPosts = List.of(noticePost);

        given(redisPostIndexAdapter.getIndexMembers(RedisKey.POST_NOTICE_IDS_KEY))
                .willReturn(Set.of(1L));
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(cachedPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 1));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        verify(redisPostIndexAdapter).getIndexMembers(RedisKey.POST_NOTICE_IDS_KEY);
    }

    @ParameterizedTest(name = "{0} - SET 인덱스 비어있음 → 빈 페이지 반환")
    @MethodSource("provideCacheEmptyScenarios")
    @DisplayName("SET 인덱스 비어있음 - 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenIndexEmpty(String indexKey, PostCacheFlag flag) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        given(redisPostIndexAdapter.getIndexMembers(indexKey)).willReturn(Set.of());

        // When
        Page<PostSimpleDetail> result = switch (flag) {
            case WEEKLY -> featuredPostCacheService.getWeeklyPosts(pageable);
            case LEGEND -> featuredPostCacheService.getPopularPostLegend(pageable);
            case NOTICE -> featuredPostCacheService.getNoticePosts(pageable);
        };

        // Then: 빈 페이지 즉시 반환
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @ParameterizedTest(name = "{1} - Redis 장애 시 DB fallback (featuredType 기반)")
    @MethodSource("provideRedisFallbackScenarios")
    @DisplayName("Redis 장애 시 DB fallback (featuredType 기반)")
    void shouldFallbackToDb_WhenRedisFails(String indexKey, PostCacheFlag cacheFlag, String expectedTitle) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, expectedTitle);

        given(redisPostIndexAdapter.getIndexMembers(indexKey))
                .willThrow(new RuntimeException("Redis connection failed"));

        given(postQueryRepository.findPostsByFeaturedType(eq(cacheFlag), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post), pageable, 1));

        // When
        Page<PostSimpleDetail> result = switch (cacheFlag) {
            case WEEKLY -> featuredPostCacheService.getWeeklyPosts(pageable);
            case LEGEND -> featuredPostCacheService.getPopularPostLegend(pageable);
            case NOTICE -> featuredPostCacheService.getNoticePosts(pageable);
        };

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(expectedTitle);
        verify(postQueryRepository).findPostsByFeaturedType(eq(cacheFlag), any(Pageable.class));
    }

    static Stream<Arguments> provideCacheEmptyScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_IDS_KEY, PostCacheFlag.WEEKLY),
                Arguments.of(RedisKey.POST_LEGEND_IDS_KEY, PostCacheFlag.LEGEND),
                Arguments.of(RedisKey.POST_NOTICE_IDS_KEY, PostCacheFlag.NOTICE)
        );
    }

    static Stream<Arguments> provideRedisFallbackScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_IDS_KEY, PostCacheFlag.WEEKLY, "주간 인기글"),
                Arguments.of(RedisKey.POST_LEGEND_IDS_KEY, PostCacheFlag.LEGEND, "레전드 게시글"),
                Arguments.of(RedisKey.POST_NOTICE_IDS_KEY, PostCacheFlag.NOTICE, "공지사항")
        );
    }

    // ==================== Hash 캐시 미스 복구 ====================

    @Test
    @DisplayName("일부 Hash 미스 시 DB 조회 후 Hash 생성하여 복구")
    void shouldRecoverFromPartialCacheMiss() {
        // Given: SET 인덱스에 3개 ID가 있지만 Hash는 2개만 존재
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail cachedPost1 = PostTestDataBuilder.createPostSearchResult(1L, "캐시된 글 1");
        PostSimpleDetail cachedPost2 = PostTestDataBuilder.createPostSearchResult(2L, "캐시된 글 2");
        PostSimpleDetail dbPost = PostTestDataBuilder.createPostSearchResult(3L, "DB 복구 글");

        PostDetail dbDetail = mock(PostDetail.class);
        given(dbDetail.toSimpleDetail()).willReturn(dbPost);

        given(redisPostIndexAdapter.getIndexMembers(RedisKey.POST_WEEKLY_IDS_KEY))
                .willReturn(Set.of(1L, 2L, 3L));
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(List.of(cachedPost1, cachedPost2)); // 3L 누락
        given(postQueryRepository.findPostDetail(eq(3L), isNull()))
                .willReturn(Optional.of(dbDetail));
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(dbPost, cachedPost2, cachedPost1), pageable, 3));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then: DB에서 조회한 글의 Hash가 생성됨
        assertThat(result.getContent()).hasSize(3);
        verify(redisPostHashAdapter).createPostHash(dbPost);
    }

    @Test
    @DisplayName("전체 Hash 미스 + DB에서도 없음 → 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenAllHashMissAndNoDbResult() {
        // Given: SET 인덱스에 ID가 있지만 Hash도 DB도 없음
        Pageable pageable = PageRequest.of(0, 10);

        given(redisPostIndexAdapter.getIndexMembers(RedisKey.POST_WEEKLY_IDS_KEY))
                .willReturn(Set.of(1L, 2L));
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(List.of()); // 전부 미스
        given(postQueryRepository.findPostDetail(anyLong(), isNull()))
                .willReturn(Optional.empty()); // DB에도 없음

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
