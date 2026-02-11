package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>RealtimePostCacheService 테스트</h2>
 * <p>실시간 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>JSON LIST 직접 조회, 캐시 미스 시 ZSet → DB → replaceAll 경로를 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealtimePostCacheService 테스트")
@Tag("unit")
class RealtimePostCacheServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private RedisPostJsonListAdapter redisPostJsonListAdapter;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Mock
    private PostUtil postUtil;

    private RealtimePostCacheService realtimePostCacheService;

    @BeforeEach
    void setUp() {
        realtimePostCacheService = new RealtimePostCacheService(
                postQueryRepository,
                redisRealTimePostAdapter,
                redisPostJsonListAdapter,
                realtimeScoreFallbackStore,
                postUtil
        );
    }

    @Test
    @DisplayName("실시간 인기글 조회 - JSON LIST 캐시 히트")
    void shouldGetRealtimePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");
        List<PostSimpleDetail> cachedPosts = List.of(simpleDetail2, simpleDetail1);

        given(redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(cachedPosts);
        given(postUtil.paginate(cachedPosts, pageable))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(redisPostJsonListAdapter).getAll(RedisKey.POST_REALTIME_JSON_KEY);
        verify(redisRealTimePostAdapter, never()).getRangePostId();
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 캐시 미스 시 ZSet → DB → replaceAll")
    void shouldGetRealtimePosts_CacheMiss_FillFromDB() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail dbPost1 = PostTestDataBuilder.createPostSearchResult(1L, "DB 조회 글 1");
        PostSimpleDetail dbPost2 = PostTestDataBuilder.createPostSearchResult(2L, "DB 조회 글 2");
        List<PostSimpleDetail> dbPosts = List.of(dbPost1, dbPost2);

        List<Long> ids = List.of(1L, 2L);

        given(redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(List.of());
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(ids);
        given(postQueryRepository.findPostSimpleDetailsByIds(eq(ids), any(Pageable.class)))
                .willReturn(new PageImpl<>(dbPosts));
        given(postUtil.paginate(dbPosts, pageable))
                .willReturn(new PageImpl<>(dbPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisPostJsonListAdapter).replaceAll(eq(RedisKey.POST_REALTIME_JSON_KEY), eq(dbPosts), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - ZSet 비어있음 → 빈 페이지 반환")
    void shouldGetRealtimePosts_ZSetEmpty_ReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        given(redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(List.of());
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
