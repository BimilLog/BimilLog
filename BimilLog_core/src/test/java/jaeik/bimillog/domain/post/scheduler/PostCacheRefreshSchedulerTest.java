package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.jpa.PostCacheFlag;
import jaeik.bimillog.domain.post.service.PostCacheRefresh;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostCacheRefreshScheduler 테스트</h2>
 * <p>스케줄러의 분산 락, 순차 갱신, 타입별 독립 실행을 검증합니다.</p>
 * <p>재시도 로직은 PostCacheRefresh/RealtimePostSync의 @Retryable이 담당하므로 여기서는 테스트하지 않습니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheRefreshScheduler 테스트")
@Tag("unit")
class PostCacheRefreshSchedulerTest {

    @Mock
    private PostCacheRefresh postCacheRefresh;

    @Mock
    private RealtimePostSync realtimePostSync;

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @InjectMocks
    private PostCacheRefreshScheduler scheduler;

    @Test
    @DisplayName("분산 락 획득 성공 → REALTIME, WEEKLY, LEGEND 순차 갱신")
    void shouldRefreshAllCaches_WhenLockAcquired() {
        // Given
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn(true);

        // When
        scheduler.refreshAllCaches();

        // Then
        verify(realtimePostSync).refreshRealtime();
        verify(postCacheRefresh).refreshFeatured(PostCacheFlag.WEEKLY);
        verify(postCacheRefresh).refreshFeatured(PostCacheFlag.LEGEND);
        verify(redisSimplePostAdapter).releaseSchedulerLock();
    }

    @Test
    @DisplayName("분산 락 획득 실패 → 갱신 스킵")
    void shouldSkipRefresh_WhenLockNotAcquired() {
        // Given
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn(false);

        // When
        scheduler.refreshAllCaches();

        // Then
        verify(realtimePostSync, never()).refreshRealtime();
        verify(postCacheRefresh, never()).refreshFeatured(PostCacheFlag.WEEKLY);
        verify(postCacheRefresh, never()).refreshFeatured(PostCacheFlag.LEGEND);
        verify(redisSimplePostAdapter, never()).releaseSchedulerLock();
    }

    @Test
    @DisplayName("REALTIME 갱신 실패 → WEEKLY, LEGEND 계속 진행 (safeRefresh)")
    void shouldContinueToNextType_WhenRealtimeFails() {
        // Given
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn(true);
        willThrow(new RuntimeException("Redis 연결 실패"))
                .given(realtimePostSync).refreshRealtime();

        // When
        scheduler.refreshAllCaches();

        // Then: REALTIME 실패해도 WEEKLY, LEGEND는 계속 진행
        verify(postCacheRefresh).refreshFeatured(PostCacheFlag.WEEKLY);
        verify(postCacheRefresh).refreshFeatured(PostCacheFlag.LEGEND);
        verify(redisSimplePostAdapter).releaseSchedulerLock();
    }

    @Test
    @DisplayName("모든 타입 갱신 실패해도 분산 락은 반드시 해제")
    void shouldReleaseLock_WhenAllRefreshFails() {
        // Given
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn(true);
        willThrow(new RuntimeException("예상치 못한 오류"))
                .given(realtimePostSync).refreshRealtime();
        willThrow(new RuntimeException("예상치 못한 오류"))
                .given(postCacheRefresh).refreshFeatured(PostCacheFlag.WEEKLY);
        willThrow(new RuntimeException("예상치 못한 오류"))
                .given(postCacheRefresh).refreshFeatured(PostCacheFlag.LEGEND);

        // When
        scheduler.refreshAllCaches();

        // Then: 모든 갱신 실패해도 락 해제
        verify(redisSimplePostAdapter).releaseSchedulerLock();
    }
}
