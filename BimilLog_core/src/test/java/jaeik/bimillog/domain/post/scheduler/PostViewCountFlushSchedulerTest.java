package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.service.PostInteractionService;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostViewCountFlushScheduler 단위 테스트</h2>
 * <p>조회수 플러시 스케줄러의 동작을 검증합니다.</p>
 */
@Tag("unit")
@DisplayName("PostViewCountFlushScheduler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PostViewCountFlushSchedulerTest {

    @Mock
    private RedisPostViewAdapter redisPostViewAdapter;

    @Mock
    private PostInteractionService postInteractionService;

    @InjectMocks
    private PostViewCountFlushScheduler scheduler;

    @Test
    @DisplayName("락 획득 실패 시 플러시 건너뜀")
    void shouldSkipFlush_whenLockNotAcquired() {
        // Given
        given(redisPostViewAdapter.tryAcquireFlushLock()).willReturn(false);

        // When
        scheduler.flushViewCounts();

        // Then
        verify(redisPostViewAdapter, never()).getAndClearViewCounts();
        verify(postInteractionService, never()).bulkIncrementViewCounts(any());
    }

    @Test
    @DisplayName("버퍼가 비어있으면 DB 업데이트 건너뜀")
    void shouldSkipDbUpdate_whenBufferEmpty() {
        // Given
        given(redisPostViewAdapter.tryAcquireFlushLock()).willReturn(true);
        given(redisPostViewAdapter.getAndClearViewCounts()).willReturn(Collections.emptyMap());

        // When
        scheduler.flushViewCounts();

        // Then
        verify(postInteractionService, never()).bulkIncrementViewCounts(any());
    }

    @Test
    @DisplayName("버퍼에 데이터가 있으면 DB에 벌크 업데이트")
    void shouldFlushToDB_whenBufferHasData() {
        // Given
        Map<Long, Long> counts = Map.of(1L, 5L, 2L, 3L);
        given(redisPostViewAdapter.tryAcquireFlushLock()).willReturn(true);
        given(redisPostViewAdapter.getAndClearViewCounts()).willReturn(counts);

        // When
        scheduler.flushViewCounts();

        // Then
        verify(postInteractionService).bulkIncrementViewCounts(counts);
    }

    @Test
    @DisplayName("DB 업데이트 실패 시 예외를 잡아 로깅")
    void shouldCatchException_whenDbUpdateFails() {
        // Given
        Map<Long, Long> counts = Map.of(1L, 5L);
        given(redisPostViewAdapter.tryAcquireFlushLock()).willReturn(true);
        given(redisPostViewAdapter.getAndClearViewCounts()).willReturn(counts);
        doThrow(new RuntimeException("DB 오류")).when(postInteractionService).bulkIncrementViewCounts(any());

        // When - 예외가 전파되지 않아야 함
        scheduler.flushViewCounts();

        // Then
        verify(postInteractionService).bulkIncrementViewCounts(counts);
    }
}
