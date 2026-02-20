package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.PostCacheEntry;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostJsonListAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private RedisPostJsonListAdapter redisPostJsonListAdapter;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Mock
    private RealtimePostSync realtimePostSync;

    @Mock
    private PostUtil postUtil;

    @InjectMocks
    private RealtimePostCacheService realtimePostCacheService;

    // ==================== 정상 경로 ====================

    @Test
    @DisplayName("ZSet 비어있음 → 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenZSetIsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(redisPostJsonListAdapter, never()).getAll(any());
    }

    @Test
    @DisplayName("ZSet 있음 + LIST ID 순서 일치 → LIST + 카운터 결합 반환 (비동기 갱신 없음)")
    void shouldReturnListPosts_WhenIdsMatch() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        List<Long> zsetIds = List.of(2L, 1L);
        PostCacheEntry entry1 = PostTestDataBuilder.createCacheEntry(2L, "인기글1");
        PostCacheEntry entry2 = PostTestDataBuilder.createCacheEntry(1L, "인기글2");
        List<PostCacheEntry> entries = List.of(entry1, entry2);
        List<PostSimpleDetail> posts = List.of(
                PostTestDataBuilder.createPostSearchResult(2L, "인기글1"),
                PostTestDataBuilder.createPostSearchResult(1L, "인기글2")
        );

        given(redisRealTimePostAdapter.getRangePostId()).willReturn(zsetIds);
        given(redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(entries);
        given(postUtil.combineWithCounters(entries)).willReturn(posts);
        given(postUtil.paginate(any(), eq(pageable))).willReturn(new PageImpl<>(posts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(realtimePostSync, never()).asyncRebuildRealtimeCache(any());
    }

    @Test
    @DisplayName("ZSet 있음 + LIST ID 순서 불일치 → LIST + 카운터 결합 반환 + 비동기 갱신 트리거")
    void shouldReturnListAndTriggerAsyncRebuild_WhenIdsMismatch() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        List<Long> zsetIds = List.of(3L, 1L);
        PostCacheEntry entry1 = PostTestDataBuilder.createCacheEntry(1L, "인기글1");
        PostCacheEntry entry2 = PostTestDataBuilder.createCacheEntry(2L, "인기글2");
        List<PostCacheEntry> entries = List.of(entry1, entry2);
        List<PostSimpleDetail> posts = List.of(
                PostTestDataBuilder.createPostSearchResult(1L, "인기글1"),
                PostTestDataBuilder.createPostSearchResult(2L, "인기글2")
        );

        given(redisRealTimePostAdapter.getRangePostId()).willReturn(zsetIds);
        given(redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(entries);
        given(postUtil.combineWithCounters(entries)).willReturn(posts);
        given(postUtil.paginate(any(), eq(pageable))).willReturn(new PageImpl<>(posts, pageable, 2));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        verify(realtimePostSync).asyncRebuildRealtimeCache(zsetIds);
    }

    @Test
    @DisplayName("ZSet 있음 + LIST 비어있음 → 빈 카운터 결합 반환 + 비동기 갱신 트리거")
    void shouldReturnEmptyAndTriggerRebuild_WhenListIsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        List<Long> zsetIds = List.of(1L, 2L);
        List<PostCacheEntry> emptyEntries = List.of();
        List<PostSimpleDetail> emptyPosts = List.of();

        given(redisRealTimePostAdapter.getRangePostId()).willReturn(zsetIds);
        given(redisPostJsonListAdapter.getAll(RedisKey.POST_REALTIME_JSON_KEY)).willReturn(emptyEntries);
        given(postUtil.combineWithCounters(emptyEntries)).willReturn(emptyPosts);
        given(postUtil.paginate(emptyPosts, pageable)).willReturn(new PageImpl<>(emptyPosts, pageable, 0));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(realtimePostSync).asyncRebuildRealtimeCache(zsetIds);
    }
}
