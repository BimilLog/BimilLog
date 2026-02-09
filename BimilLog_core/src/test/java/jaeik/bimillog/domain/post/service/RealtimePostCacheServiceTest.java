package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisPostHashAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.domain.post.util.PostUtil;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>RealtimePostCacheService 테스트</h2>
 * <p>실시간 인기글 캐시 조회 로직을 검증합니다.</p>
 * <p>ZSET → 글 단위 Hash pipeline 조회, 캐시 미스 시 DB 조회 + Hash 생성 경로를 검증합니다.</p>
 * <p>서킷브레이커 폴백은 {@code @CircuitBreaker} 어노테이션 기반이므로 통합 테스트에서 검증합니다.</p>
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
    private RedisPostHashAdapter redisPostHashAdapter;

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
                redisPostHashAdapter,
                realtimeScoreFallbackStore,
                postUtil
        );
    }

    @Test
    @DisplayName("실시간 인기글 조회 - ZSET + 글 단위 Hash pipeline 캐시 히트")
    void shouldGetRealtimePosts_CacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail simpleDetail1 = PostTestDataBuilder.createPostSearchResult(1L, "실시간 인기글 1");
        PostSimpleDetail simpleDetail2 = PostTestDataBuilder.createPostSearchResult(2L, "실시간 인기글 2");
        List<PostSimpleDetail> cachedPosts = List.of(simpleDetail2, simpleDetail1);

        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of(2L, 1L));
        given(redisPostHashAdapter.getPostHashes(anyCollection())).willReturn(cachedPosts);
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(cachedPosts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(redisRealTimePostAdapter).getRangePostId();
        verify(redisPostHashAdapter).getPostHashes(anyCollection());
        verify(postQueryRepository, never()).findPostDetail(anyLong(), any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - 일부 Hash 미스 시 DB 조회 후 Hash 생성")
    void shouldGetRealtimePosts_PartialCacheMiss_FillFromDB() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        PostSimpleDetail cachedPost = PostTestDataBuilder.createPostSearchResult(1L, "캐시된 글");
        PostSimpleDetail dbPost = PostTestDataBuilder.createPostSearchResult(2L, "DB 조회 글");

        PostDetail dbDetail = org.mockito.Mockito.mock(PostDetail.class);
        given(dbDetail.toSimpleDetail()).willReturn(dbPost);

        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of(1L, 2L));
        given(redisPostHashAdapter.getPostHashes(anyCollection())).willReturn(List.of(cachedPost));
        given(postQueryRepository.findPostDetail(eq(2L), isNull())).willReturn(Optional.of(dbDetail));
        given(postUtil.paginate(anyList(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(cachedPost, dbPost), pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(redisPostHashAdapter).createPostHash(dbPost);
    }

    @Test
    @DisplayName("실시간 인기글 조회 - ZSET 비어있음 → 빈 페이지 반환")
    void shouldGetRealtimePosts_ZSetEmpty_ReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(redisPostHashAdapter, never()).getPostHashes(any());
    }

    @Test
    @DisplayName("실시간 인기글 조회 - Hash 전부 미스 + DB도 없음 → 빈 페이지")
    void shouldGetRealtimePosts_AllCacheMiss_NoDB_ReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of(1L, 2L));
        given(redisPostHashAdapter.getPostHashes(anyCollection())).willReturn(List.of());
        given(postQueryRepository.findPostDetail(anyLong(), isNull())).willReturn(Optional.empty());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
