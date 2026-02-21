package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListQueryAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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
 * <h2>PostPopularService 테스트</h2>
 * <p>주간/레전드/공지 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>JSON LIST → PostSimpleDetail 조회 경로를 검증합니다.</p>
 * <p>DB 폴백은 독립 boolean 플래그 기반 쿼리를 사용합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostPopularService 테스트")
@Tag("unit")
class PostPopularServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisPostListQueryAdapter redisPostListQueryAdapter;

    @Mock
    private PostUtil postUtil;

    private PostPopularService postPopularService;

    @BeforeEach
    void setUp() {
        postPopularService = new PostPopularService(
                postQueryRepository,
                redisPostListQueryAdapter,
                postUtil
        );
    }

    @ParameterizedTest(name = "{1} - JSON LIST 캐시 히트")
    @MethodSource("provideCacheHitScenarios")
    @DisplayName("주간/레전드/공지 캐시 히트")
    void shouldGetPopularPosts_CacheHit(String jsonKey, String label, String titlePrefix, PostQueryType type) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<PostSimpleDetail> posts = List.of(
                PostTestDataBuilder.createPostSearchResult(1L, titlePrefix + " 1"),
                PostTestDataBuilder.createPostSearchResult(2L, titlePrefix + " 2")
        );

        given(redisPostListQueryAdapter.getAll(jsonKey)).willReturn(posts);
        given(postUtil.paginate(any(), eq(pageable)))
                .willReturn(new PageImpl<>(posts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = postPopularService.getPopularPosts(pageable, jsonKey, type);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisPostListQueryAdapter).getAll(jsonKey);
    }

    @ParameterizedTest(name = "{1} - JSON LIST 비어있음 → Page.empty() 반환")
    @MethodSource("provideCacheEmptyScenarios")
    @DisplayName("JSON LIST 비어있음 - DB 폴백 없이 빈 결과 반환")
    void shouldFallbackToDb_WhenCacheEmpty(String jsonKey, String label, PostQueryType type) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        given(redisPostListQueryAdapter.getAll(jsonKey)).willReturn(Collections.emptyList());

        // When: 캐시가 비어있으면 getCachedPosts()에서 Page.empty()를 바로 반환 (DB 폴백 아님)
        Page<PostSimpleDetail> result = postPopularService.getPopularPosts(pageable, jsonKey, type);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(postQueryRepository);
    }

    @ParameterizedTest(name = "{1} - Redis 장애 시 DB fallback")
    @MethodSource("provideRedisFallbackScenarios")
    @DisplayName("Redis 장애 시 DB fallback")
    void shouldFallbackToDb_WhenRedisFails(String jsonKey, String label, String expectedTitle, PostQueryType type) {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail post = PostTestDataBuilder.createPostSearchResult(1L, expectedTitle);

        given(redisPostListQueryAdapter.getAll(jsonKey))
                .willThrow(new RuntimeException("Redis connection failed"));
        given(postQueryRepository.selectPostSimpleDetails(any(), eq(pageable), any()))
                .willReturn(new PageImpl<>(List.of(post), pageable, 1));

        // When
        Page<PostSimpleDetail> result = postPopularService.getPopularPosts(pageable, jsonKey, type);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(expectedTitle);
        verify(postQueryRepository).selectPostSimpleDetails(any(), eq(pageable), any());
    }

    static Stream<Arguments> provideCacheHitScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_JSON_KEY, "WEEKLY", "주간 인기글", PostQueryType.WEEKLY),
                Arguments.of(RedisKey.POST_LEGEND_JSON_KEY, "LEGEND", "레전드 게시글", PostQueryType.LEGEND),
                Arguments.of(RedisKey.POST_NOTICE_JSON_KEY, "NOTICE", "공지사항", PostQueryType.NOTICE)
        );
    }

    static Stream<Arguments> provideCacheEmptyScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_JSON_KEY, "WEEKLY", PostQueryType.WEEKLY),
                Arguments.of(RedisKey.POST_LEGEND_JSON_KEY, "LEGEND", PostQueryType.LEGEND),
                Arguments.of(RedisKey.POST_NOTICE_JSON_KEY, "NOTICE", PostQueryType.NOTICE)
        );
    }

    static Stream<Arguments> provideRedisFallbackScenarios() {
        return Stream.of(
                Arguments.of(RedisKey.POST_WEEKLY_JSON_KEY, "WEEKLY", "주간 인기글", PostQueryType.WEEKLY),
                Arguments.of(RedisKey.POST_LEGEND_JSON_KEY, "LEGEND", "레전드 게시글", PostQueryType.LEGEND),
                Arguments.of(RedisKey.POST_NOTICE_JSON_KEY, "NOTICE", "공지사항", PostQueryType.NOTICE)
        );
    }

}
