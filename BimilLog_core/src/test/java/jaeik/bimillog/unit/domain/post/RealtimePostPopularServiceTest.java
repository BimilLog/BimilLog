package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostEvent.RealtimeCacheRebuildEvent;
import jaeik.bimillog.domain.post.service.RealtimePostCacheService;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListQueryAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>RealtimePostCacheService 테스트</h2>
 * <p>ZSet → LIST 비교 기반 실시간 인기글 조회 로직을 검증합니다.</p>
 * <p>ZSet/LIST 비교, 비동기 갱신 트리거, 폴백 경로를 검증합니다.</p>
 * <p>서킷브레이커 폴백은 @CircuitBreaker AOP로 동작하므로 단위 테스트에서는 정상 경로만 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealtimePostCacheService 테스트")
@Tag("unit")
class RealtimePostPopularServiceTest {

    @Mock
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @Mock
    private RedisPostListQueryAdapter redisPostListQueryAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PostUtil postUtil;

    @InjectMocks
    private RealtimePostCacheService realtimePostCacheService;

    // ==================== 정상 경로 ====================

    @Test
    @DisplayName("ZSet 비어있음 → 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenZSetIsEmpty() {
        // Given
        given(redisPostRealTimeAdapter.getRangePostId()).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts();

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(redisPostListQueryAdapter, never()).getAll(any());
    }

    @Test
    @DisplayName("ZSet 있음 + LIST ID 순서 일치 → LIST + 카운터 결합 반환 (비동기 갱신 없음)")
    void shouldReturnListPosts_WhenIdsMatch() {
        // Given
        List<Long> zsetIds = List.of(2L, 1L);
        List<PostSimpleDetail> posts = List.of(
                PostTestDataBuilder.createPostSearchResult(2L, "인기글1"),
                PostTestDataBuilder.createPostSearchResult(1L, "인기글2")
        );

        given(redisPostRealTimeAdapter.getRangePostId()).willReturn(zsetIds);
        given(redisPostListQueryAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(posts);
        given(postUtil.paginate(any(), any(Pageable.class))).willReturn(new PageImpl<>(posts));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts();

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(eventPublisher, never()).publishEvent(any(RealtimeCacheRebuildEvent.class));
    }

    @Test
    @DisplayName("ZSet 있음 + LIST ID 순서 불일치 → LIST + 카운터 결합 반환 + 비동기 갱신 트리거")
    void shouldReturnListAndTriggerAsyncRebuild_WhenIdsMismatch() {
        // Given
        List<Long> zsetIds = List.of(3L, 1L);
        List<PostSimpleDetail> posts = List.of(
                PostTestDataBuilder.createPostSearchResult(1L, "인기글1"),
                PostTestDataBuilder.createPostSearchResult(2L, "인기글2")
        );

        given(redisPostRealTimeAdapter.getRangePostId()).willReturn(zsetIds);
        given(redisPostListQueryAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(posts);
        given(postUtil.paginate(any(), any(Pageable.class))).willReturn(new PageImpl<>(posts));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts();

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(eventPublisher).publishEvent(any(RealtimeCacheRebuildEvent.class));
    }

    @Test
    @DisplayName("ZSet 있음 + LIST 비어있음 → 빈 카운터 결합 반환 + 비동기 갱신 트리거")
    void shouldReturnEmptyAndTriggerRebuild_WhenListIsEmpty() {
        // Given
        List<Long> zsetIds = List.of(1L, 2L);
        List<PostSimpleDetail> emptyPosts = List.of();

        given(redisPostRealTimeAdapter.getRangePostId()).willReturn(zsetIds);
        given(redisPostListQueryAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(emptyPosts);
        given(postUtil.paginate(any(), any(Pageable.class))).willReturn(new PageImpl<>(emptyPosts));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts();

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(eventPublisher).publishEvent(any(RealtimeCacheRebuildEvent.class));
    }
}
