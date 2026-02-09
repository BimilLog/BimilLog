package jaeik.bimillog.domain.post.scheduler;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisSimplePostAdapter;
import jaeik.bimillog.infrastructure.resilience.RealtimeScoreFallbackStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>RealTimePostScheduler 테스트</h2>
 * <p>실시간 인기글 캐시 갱신 스케줄러의 분산 락, 서킷 분기, 갱신, 재시도, 예외 처리를 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealTimePostScheduler 테스트")
@Tag("unit")
class RealTimePostSchedulerTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisSimplePostAdapter redisSimplePostAdapter;

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private RealTimePostScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(circuitBreakerRegistry.circuitBreaker("realtimeRedis")).thenReturn(circuitBreaker);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("분산 락 획득 성공 → 서킷 닫힘 → Redis에서 ID 조회 후 캐시 갱신")
    void shouldRefreshRealtimeCache_WhenLockAcquiredAndCircuitClosed() {
        // Given
        PostDetail detail1 = mockPostDetail();
        PostDetail detail2 = mockPostDetail();
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn("test-uuid");
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of(1L, 2L));
        given(postQueryRepository.findPostDetail(eq(1L), isNull())).willReturn(Optional.of(detail1));
        given(postQueryRepository.findPostDetail(eq(2L), isNull())).willReturn(Optional.of(detail2));

        // When
        scheduler.refreshRealtimeCache();

        // Then
        verify(redisSimplePostAdapter).cachePostsWithTtl(eq(RedisKey.REALTIME_SIMPLE_KEY), anyList(), isNull());
        verify(redisSimplePostAdapter).releaseSchedulerLock("test-uuid");
    }

    @Test
    @DisplayName("서킷 OPEN → Caffeine 폴백에서 ID 조회 후 캐시 갱신")
    void shouldUseCaffeineFallback_WhenCircuitOpen() {
        // Given
        PostDetail detail = mockPostDetail();
        given(circuitBreaker.getState()).willReturn(CircuitBreaker.State.OPEN);
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn("test-uuid");
        given(realtimeScoreFallbackStore.getTopPostIds(0, 5)).willReturn(List.of(1L));
        given(postQueryRepository.findPostDetail(eq(1L), isNull())).willReturn(Optional.of(detail));

        // When
        scheduler.refreshRealtimeCache();

        // Then
        verify(redisRealTimePostAdapter, never()).getRangePostId();
        verify(realtimeScoreFallbackStore).getTopPostIds(0, 5);
        verify(redisSimplePostAdapter).cachePostsWithTtl(eq(RedisKey.REALTIME_SIMPLE_KEY), anyList(), isNull());
    }

    @Test
    @DisplayName("분산 락 획득 실패 → 갱신 스킵")
    void shouldSkipRefresh_WhenLockNotAcquired() {
        // Given
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn(null);

        // When
        scheduler.refreshRealtimeCache();

        // Then
        verify(redisRealTimePostAdapter, never()).getRangePostId();
        verify(redisSimplePostAdapter, never()).releaseSchedulerLock(anyString());
    }

    @Test
    @DisplayName("갱신 실패해도 분산 락은 반드시 해제")
    void shouldReleaseLock_WhenRefreshFails() {
        // Given
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn("test-uuid");
        willThrow(new RuntimeException("Redis 연결 실패"))
                .given(redisRealTimePostAdapter).getRangePostId();

        // When
        scheduler.refreshRealtimeCache();

        // Then
        verify(redisSimplePostAdapter).releaseSchedulerLock("test-uuid");
    }

    @Test
    @DisplayName("빈 ID 목록 → 캐시 갱신 스킵")
    void shouldSkipRefresh_WhenPostIdsEmpty() {
        // Given
        given(redisSimplePostAdapter.tryAcquireSchedulerLock()).willReturn("test-uuid");
        given(redisRealTimePostAdapter.getRangePostId()).willReturn(List.of());

        // When
        scheduler.refreshRealtimeCache();

        // Then
        verify(postQueryRepository, never()).findPostDetail(anyLong(), any());
        verify(redisSimplePostAdapter).releaseSchedulerLock("test-uuid");
    }

    private PostDetail mockPostDetail() {
        PostDetail detail = mock(PostDetail.class);
        PostSimpleDetail simpleDetail = mock(PostSimpleDetail.class);
        given(detail.toSimpleDetail()).willReturn(simpleDetail);
        return detail;
    }
}
