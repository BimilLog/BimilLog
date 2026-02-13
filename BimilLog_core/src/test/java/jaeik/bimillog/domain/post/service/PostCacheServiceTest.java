package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostCounterAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
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
 * <h2>PostCacheService 테스트</h2>
 * <p>주간/레전드/공지 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>JSON LIST(PostCacheEntry) + 카운터 Hash(HMGET) → PostSimpleDetail 결합 경로를 검증합니다.</p>
 * <p>DB 폴백은 독립 boolean 플래그 기반 쿼리를 사용합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheService 테스트")
@Tag("unit")
class PostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisPostJsonListAdapter redisPostJsonListAdapter;

    @Mock
    private RedisPostCounterAdapter redisPostCounterAdapter;

    @Mock
    private PostUtil postUtil;

    private PostCacheService postCacheService;

    @BeforeEach
    void setUp() {
        postCacheService = new PostCacheService(
                postQueryRepository,
                redisPostJsonListAdapter,
                redisPostCounterAdapter,
                postUtil
        );
    }

    @Test
    @DisplayName("주간 인기글 조회 - JSON LIST 캐시 히트")
    void shouldGetWeeklyPosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostCacheEntry entry1 = PostTestDataBuilder.createCacheEntry(1L, "주간 인기글 1");
        PostCacheEntry entry2 = PostTestDataBuilder.createCacheEntry(2L, "주간 인기글 2");
        List<PostCacheEntry> entries = List.of(entry1, entry2);

        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "주간 인기글 1");
        PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "주간 인기글 2");
        List<PostSimpleDetail> combinedPosts = List.of(post1, post2);

        given(redisPostJsonListAdapter.getAll(RedisKey.POST_WEEKLY_JSON_KEY)).willReturn(entries);
        given(redisPostCounterAdapter.combineWithCounters(entries)).willReturn(combinedPosts);
        given(postUtil.paginate(combinedPosts, pageable))
                .willReturn(new PageImpl<>(combinedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getWeeklyPosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisPostJsonListAdapter).getAll(RedisKey.POST_WEEKLY_JSON_KEY);
        verify(redisPostCounterAdapter).combineWithCounters(entries);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 히트")
    void shouldGetPopularPostLegend() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostCacheEntry entry1 = PostTestDataBuilder.createCacheEntry(1L, "레전드 게시글 1");
        PostCacheEntry entry2 = PostTestDataBuilder.createCacheEntry(2L, "레전드 게시글 2");
        List<PostCacheEntry> entries = List.of(entry1, entry2);

        PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(1L, "레전드 게시글 1");
        PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "레전드 게시글 2");
        List<PostSimpleDetail> combinedPosts = List.of(post1, post2);

        given(redisPostJsonListAdapter.getAll(RedisKey.POST_LEGEND_JSON_KEY)).willReturn(entries);
        given(redisPostCounterAdapter.combineWithCounters(entries)).willReturn(combinedPosts);
        given(postUtil.paginate(combinedPosts, pageable))
                .willReturn(new PageImpl<>(combinedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = postCacheService.getPopularPostLegend(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisPostJsonListAdapter).getAll(RedisKey.POST_LEGEND_JSON_KEY);
        verify(redisPostCounterAdapter).combineWithCounters(entries);
    }

    @Test
    @DisplayName("공지사항 조회 - 캐시 히트")
    void shouldGetNoticePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostCacheEntry entry = PostTestDataBuilder.createCacheEntry(1L, "공지사항");
        List<PostCacheEntry> entries = List.of(entry);

        PostSimpleDetail noticePost = PostTestDataBuilder.createPostSearchResult(1L, "공지사항");
        List<PostSimpleDetail> combinedPosts = List.of(noticePost);

        given(redisPostJsonListAdapter.getAll(RedisKey.POST_NOTICE_JSON_KEY)).willReturn(entries);
        given(redisPostCounterAdapter.combineWithCounters(entries)).willReturn(combinedPosts);
        given(postUtil.paginate(combinedPosts, pageable))
                .willReturn(new PageImpl<>(combinedPosts, pageable, 1));

        // When
        Page<PostSimpleDetail> result = postCacheService.getNoticePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        verify(redisPostJsonListAdapter).getAll(RedisKey.POST_NOTICE_JSON_KEY);
        verify(redisPostCounterAdapter).combineWithCounters(entries);
    }

    @ParameterizedTest(name = "{1} - JSON LIST 비어있음 → DB 폴백")
    @MethodSource("provideCacheEmptyScenarios")
    @DisplayName("JSON LIST 비어있음 - DB 폴백")
    void shouldFallbackToDb_WhenCacheEmpty(String jsonKey, String label) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        given(redisPostJsonListAdapter.getAll(jsonKey)).willReturn(Collections.emptyList());

        switch (label) {
            case "WEEKLY" -> given(postQueryRepository.findWeeklyPostsFallback(any(Pageable.class)))
                    .willReturn(Page.empty());
            case "LEGEND" -> given(postQueryRepository.findLegendPostsFallback(any(Pageable.class)))
                    .willReturn(Page.empty());
            case "NOTICE" -> given(postQueryRepository.findNoticePostsFallback(any(Pageable.class)))
                    .willReturn(Page.empty());
        }

        // When
        Page<PostSimpleDetail> result = switch (label) {
            case "WEEKLY" -> postCacheService.getWeeklyPosts(pageable);
            case "LEGEND" -> postCacheService.getPopularPostLegend(pageable);
            case "NOTICE" -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unknown label: " + label);
        };

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @ParameterizedTest(name = "{1} - Redis 장애 시 DB fallback")
    @MethodSource("provideRedisFallbackScenarios")
    @DisplayName("Redis 장애 시 DB fallback")
    void shouldFallbackToDb_WhenRedisFails(String jsonKey, String label, String expectedTitle) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, expectedTitle);

        given(redisPostJsonListAdapter.getAll(jsonKey))
                .willThrow(new RuntimeException("Redis connection failed"));

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
            case "WEEKLY" -> postCacheService.getWeeklyPosts(pageable);
            case "LEGEND" -> postCacheService.getPopularPostLegend(pageable);
            case "NOTICE" -> postCacheService.getNoticePosts(pageable);
            default -> throw new IllegalArgumentException("Unknown label: " + label);
        };

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(expectedTitle);
    }

    static Stream<Arguments> provideCacheEmptyScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_JSON_KEY, "WEEKLY"),
                Arguments.of(RedisKey.POST_LEGEND_JSON_KEY, "LEGEND"),
                Arguments.of(RedisKey.POST_NOTICE_JSON_KEY, "NOTICE")
        );
    }

    static Stream<Arguments> provideRedisFallbackScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_JSON_KEY, "WEEKLY", "주간 인기글"),
                Arguments.of(RedisKey.POST_LEGEND_JSON_KEY, "LEGEND", "레전드 게시글"),
                Arguments.of(RedisKey.POST_NOTICE_JSON_KEY, "NOTICE", "공지사항")
        );
    }

    // ==================== 첫 페이지 캐시 조회 (JSON LIST 방식) ====================

    @Nested
    @DisplayName("첫 페이지 캐시 조회 (JSON LIST)")
    class FirstPageTests {

        @Test
        @DisplayName("첫 페이지 캐시 히트 - JSON LIST + 카운터 결합")
        void shouldGetFirstPagePosts_CacheHit() {
            // Given
            PostCacheEntry entry1 = PostTestDataBuilder.createCacheEntry(3L, "최신글");
            PostCacheEntry entry2 = PostTestDataBuilder.createCacheEntry(2L, "두번째글");
            PostCacheEntry entry3 = PostTestDataBuilder.createCacheEntry(1L, "세번째글");
            List<PostCacheEntry> entries = List.of(entry1, entry2, entry3);

            PostSimpleDetail post1 = PostTestDataBuilder.createPostSearchResult(3L, "최신글");
            PostSimpleDetail post2 = PostTestDataBuilder.createPostSearchResult(2L, "두번째글");
            PostSimpleDetail post3 = PostTestDataBuilder.createPostSearchResult(1L, "세번째글");
            List<PostSimpleDetail> combinedPosts = List.of(post1, post2, post3);

            given(redisPostJsonListAdapter.getAll(RedisKey.FIRST_PAGE_JSON_KEY)).willReturn(entries);
            given(redisPostCounterAdapter.combineWithCounters(entries)).willReturn(combinedPosts);

            // When
            List<PostSimpleDetail> result = postCacheService.getFirstPagePosts();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getTitle()).isEqualTo("최신글");
            verify(redisPostJsonListAdapter).getAll(RedisKey.FIRST_PAGE_JSON_KEY);
            verify(redisPostCounterAdapter).combineWithCounters(entries);
            verify(postQueryRepository, never()).findBoardPostsByCursor(any(), anyInt());
        }

        @Test
        @DisplayName("첫 페이지 캐시 미스 (빈 JSON LIST) - DB 폴백")
        void shouldGetFirstPagePosts_CacheMiss_DbFallback() {
            // Given
            PostSimpleDetail dbPost = PostTestDataBuilder.createPostSearchResult(1L, "DB 폴백 글");
            List<PostSimpleDetail> dbPosts = List.of(dbPost);

            given(redisPostJsonListAdapter.getAll(RedisKey.FIRST_PAGE_JSON_KEY)).willReturn(Collections.emptyList());
            given(postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE)).willReturn(dbPosts);

            // When
            List<PostSimpleDetail> result = postCacheService.getFirstPagePosts();

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

            given(redisPostJsonListAdapter.getAll(RedisKey.FIRST_PAGE_JSON_KEY))
                    .willThrow(new RuntimeException("Redis connection failed"));
            given(postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE)).willReturn(dbPosts);

            // When
            List<PostSimpleDetail> result = postCacheService.getFirstPagePosts();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("DB 폴백 글");
            verify(postQueryRepository).findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE);
        }
    }
}
