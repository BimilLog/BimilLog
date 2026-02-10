package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostIndexAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FeaturedPostCacheService 테스트</h2>
 * <p>주간/레전드/공지 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>List 인덱스 → 글 단위 Hash pipeline 조회, 캐시 미스 시 빈 페이지 반환, 예외 시 DB 폴백 경로를 검증합니다.</p>
 * <p>DB 폴백은 독립 boolean 플래그 기반 쿼리를 사용합니다.</p>
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
    private RedisFirstPagePostAdapter redisFirstPagePostAdapter;

    @Mock
    private PostUtil postUtil;

    private FeaturedPostCacheService featuredPostCacheService;

    @BeforeEach
    void setUp() {
        featuredPostCacheService = new FeaturedPostCacheService(
                postQueryRepository,
                redisPostHashAdapter,
                redisPostIndexAdapter,
                redisFirstPagePostAdapter,
                postUtil
        );
    }

    @Test
    @DisplayName("주간 인기글 조회 - List 인덱스 + 글 단위 Hash 캐시 히트")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail weeklyPost1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail weeklyPost2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");
        List<Long> ids = List.of(1L, 2L);
        List<PostSimpleDetail> cachedPosts = List.of(weeklyPost1, weeklyPost2);

        given(redisPostIndexAdapter.getIndexList(RedisKey.POST_WEEKLY_IDS_KEY))
                .willReturn(ids);
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(cachedPosts);
        given(postUtil.recoverMissingHashes(ids, cachedPosts)).willReturn(cachedPosts);
        given(postUtil.orderByIds(ids, cachedPosts)).willReturn(cachedPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisPostIndexAdapter).getIndexList(RedisKey.POST_WEEKLY_IDS_KEY);
        verify(redisPostHashAdapter).getPostHashes(anyCollection());
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 히트")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail legendPost1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail legendPost2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        List<Long> ids = List.of(1L, 2L);
        List<PostSimpleDetail> cachedPosts = List.of(legendPost1, legendPost2);

        given(redisPostIndexAdapter.getIndexList(RedisKey.POST_LEGEND_IDS_KEY))
                .willReturn(ids);
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(cachedPosts);
        given(postUtil.recoverMissingHashes(ids, cachedPosts)).willReturn(cachedPosts);
        given(postUtil.orderByIds(ids, cachedPosts)).willReturn(cachedPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisPostIndexAdapter).getIndexList(RedisKey.POST_LEGEND_IDS_KEY);
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<Long> ids = List.of(1L);
        List<PostSimpleDetail> cachedPosts = List.of(noticePost);

        given(redisPostIndexAdapter.getIndexList(RedisKey.POST_NOTICE_IDS_KEY))
                .willReturn(ids);
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(cachedPosts);
        given(postUtil.recoverMissingHashes(ids, cachedPosts)).willReturn(cachedPosts);
        given(postUtil.orderByIds(ids, cachedPosts)).willReturn(cachedPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 1));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        verify(redisPostIndexAdapter).getIndexList(RedisKey.POST_NOTICE_IDS_KEY);
    }

    @ParameterizedTest(name = "{0} - List 인덱스 비어있음 → 빈 페이지 반환")
    @MethodSource("provideCacheEmptyScenarios")
    @DisplayName("List 인덱스 비어있음 - 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenIndexEmpty(String indexKey, String label) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        given(redisPostIndexAdapter.getIndexList(indexKey)).willReturn(Collections.emptyList());

        // When
        Page<PostSimpleDetail> result = switch (label) {
            case "WEEKLY" -> featuredPostCacheService.getWeeklyPosts(pageable);
            case "LEGEND" -> featuredPostCacheService.getPopularPostLegend(pageable);
            case "NOTICE" -> featuredPostCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unknown label: " + label);
        };

        // Then: 빈 페이지 즉시 반환
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @ParameterizedTest(name = "{1} - Redis 장애 시 DB fallback (boolean 플래그 기반)")
    @MethodSource("provideRedisFallbackScenarios")
    @DisplayName("Redis 장애 시 DB fallback (boolean 플래그 기반)")
    void shouldFallbackToDb_WhenRedisFails(String indexKey, String label, String expectedTitle) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, expectedTitle);

        given(redisPostIndexAdapter.getIndexList(indexKey))
                .willThrow(new RuntimeException("Redis connection failed"));

        // 각 폴백 메서드 mock
        switch (label) {
            case "WEEKLY" -> given(postQueryRepository.findWeeklyPostsFallback(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(post), pageable, 1));
            case "LEGEND" -> given(postQueryRepository.findLegendPostsFallback(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(post), pageable, 1));
            case "NOTICE" -> given(postQueryRepository.findNoticePostsFallback(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(post), pageable, 1));
        }

        // When
        Page<PostSimpleDetail> result = switch (label) {
            case "WEEKLY" -> featuredPostCacheService.getWeeklyPosts(pageable);
            case "LEGEND" -> featuredPostCacheService.getPopularPostLegend(pageable);
            case "NOTICE" -> featuredPostCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unknown label: " + label);
        };

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(expectedTitle);
    }

    static Stream<Arguments> provideCacheEmptyScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_IDS_KEY, "WEEKLY"),
                Arguments.of(RedisKey.POST_LEGEND_IDS_KEY, "LEGEND"),
                Arguments.of(RedisKey.POST_NOTICE_IDS_KEY, "NOTICE")
        );
    }

    static Stream<Arguments> provideRedisFallbackScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_IDS_KEY, "WEEKLY", "주간 인기글"),
                Arguments.of(RedisKey.POST_LEGEND_IDS_KEY, "LEGEND", "레전드 게시글"),
                Arguments.of(RedisKey.POST_NOTICE_IDS_KEY, "NOTICE", "공지사항")
        );
    }

    // ==================== Hash 캐시 미스 복구 ====================

    @Test
    @DisplayName("일부 Hash 미스 시 DB 조회 후 Hash 생성하여 복구")
    void shouldRecoverFromPartialCacheMiss() {
        // Given: List 인덱스에 3개 ID가 있지만 Hash는 2개만 존재
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail cachedPost1 = PostTestDataBuilder.createPostSearchResult(1L, "캐시된 글 1");
        PostSimpleDetail cachedPost2 = PostTestDataBuilder.createPostSearchResult(2L, "캐시된 글 2");
        PostSimpleDetail dbPost = PostTestDataBuilder.createPostSearchResult(3L, "DB 복구 글");
        List<Long> ids = List.of(1L, 2L, 3L);
        List<PostSimpleDetail> partialPosts = List.of(cachedPost1, cachedPost2);
        List<PostSimpleDetail> recoveredPosts = List.of(cachedPost1, cachedPost2, dbPost);

        given(redisPostIndexAdapter.getIndexList(RedisKey.POST_WEEKLY_IDS_KEY))
                .willReturn(ids);
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(partialPosts);
        given(postUtil.recoverMissingHashes(ids, partialPosts)).willReturn(recoveredPosts);
        given(postUtil.orderByIds(ids, recoveredPosts)).willReturn(recoveredPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(recoveredPosts, pageable, 3));

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then: postUtil.recoverMissingHashes가 복구 수행
        assertThat(result.getContent()).hasSize(3);
        verify(postUtil).recoverMissingHashes(ids, partialPosts);
    }

    @Test
    @DisplayName("전체 Hash 미스 + DB에서도 없음 → 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenAllHashMissAndNoDbResult() {
        // Given: List 인덱스에 ID가 있지만 Hash도 DB도 없음
        Pageable pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L, 2L);

        given(redisPostIndexAdapter.getIndexList(RedisKey.POST_WEEKLY_IDS_KEY))
                .willReturn(ids);
        given(redisPostHashAdapter.getPostHashes(anyCollection()))
                .willReturn(List.of()); // 전부 미스
        given(postUtil.recoverMissingHashes(eq(ids), anyList()))
                .willReturn(List.of()); // 복구 후에도 빈 리스트

        // When
        Page<PostSimpleDetail> result = featuredPostCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ==================== 첫 페이지 캐시 조회 ====================

    @Nested
    @DisplayName("첫 페이지 캐시 조회")
    class FirstPageTests {

        @Test
        @DisplayName("첫 페이지 캐시 히트 - List + Hash 파이프라인")
        void shouldGetFirstPagePosts_CacheHit() {
            // Given
            List<Long> ids = List.of(3L, 2L, 1L);
            PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(3L, "최신글");
            PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "두번째글");
            PostSimpleDetail post3 = PostTestDataBuilder.createPostSearchResult(1L, "세번째글");
            List<PostSimpleDetail> cachedPosts = List.of(post1, post2, post3);

            given(redisFirstPagePostAdapter.getFirstPageIds()).willReturn(ids);
            given(redisPostHashAdapter.getPostHashes(ids)).willReturn(cachedPosts);
            given(postUtil.recoverMissingHashes(ids, cachedPosts)).willReturn(cachedPosts);
            given(postUtil.orderByIds(ids, cachedPosts)).willReturn(cachedPosts);

            // When
            List<PostSimpleDetail> result = featuredPostCacheService.getFirstPagePosts();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getTitle()).isEqualTo("최신글");
            verify(redisFirstPagePostAdapter).getFirstPageIds();
            verify(redisPostHashAdapter).getPostHashes(ids);
            verify(postQueryRepository, never()).findBoardPostsByCursor(any(), anyInt());
        }

        @Test
        @DisplayName("첫 페이지 캐시 미스 (빈 ID 목록) - DB 폴백")
        void shouldGetFirstPagePosts_CacheMiss_DbFallback() {
            // Given
            PostSimpleDetail dbPost = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");
            List<PostSimpleDetail> dbPosts = List.of(dbPost);

            given(redisFirstPagePostAdapter.getFirstPageIds()).willReturn(Collections.emptyList());
            given(postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE)).willReturn(dbPosts);

            // When
            List<PostSimpleDetail> result = featuredPostCacheService.getFirstPagePosts();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("DB 폴백 글");
            verify(postQueryRepository).findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
        }

        @Test
        @DisplayName("첫 페이지 Redis 장애 - DB 폴백")
        void shouldGetFirstPagePosts_RedisFailure_DbFallback() {
            // Given
            PostSimpleDetail dbPost = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");
            List<PostSimpleDetail> dbPosts = List.of(dbPost);

            given(redisFirstPagePostAdapter.getFirstPageIds())
                    .willThrow(new RuntimeException("Redis connection failed"));
            given(postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE)).willReturn(dbPosts);

            // When
            List<PostSimpleDetail> result = featuredPostCacheService.getFirstPagePosts();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("DB 폴백 글");
            verify(postQueryRepository).findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
        }
    }
}
