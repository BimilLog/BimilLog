package jaeik.bimillog.domain.post.scheduler;

import com.querydsl.core.types.dsl.NumberPath;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListDeleteAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCacheViewScheduler 단위 테스트</h2>
 * <p>카운트 플러시 스케줄러(조회수)의 동작을 검증합니다.</p>
 * <p>DB + JSON LIST 카운터 동시 반영을 검증합니다.</p>
 */
@Tag("unit")
@DisplayName("PostCacheViewScheduler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PostCacheViewSchedulerTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisPostViewAdapter redisPostViewAdapter;

    @Mock
    private RedisPostListDeleteAdapter redisPostListDeleteAdapter;

    @InjectMocks
    private PostCacheViewScheduler scheduler;

    // ==================== 조회수 ====================

    @Test
    @DisplayName("조회수 버퍼가 비어있으면 DB 업데이트 건너뜀")
    void shouldSkipViewCountDbUpdate_whenBufferEmpty() {
        // Given
        given(redisPostViewAdapter.getAndClearViewCounts()).willReturn(Collections.emptyMap());

        // When
        scheduler.flushAllCounts();

        // Then
        verify(postQueryRepository, never()).bulkIncrementCount(any(), any());
    }

    @Test
    @DisplayName("조회수 버퍼에 데이터가 있으면 DB + JSON LIST 카운터에 벌크 업데이트")
    void shouldFlushViewCountsToDB_whenBufferHasData() {
        // Given
        Map<Long, Long> viewCounts = Map.of(1L, 5L, 2L, 3L);
        given(redisPostViewAdapter.getAndClearViewCounts()).willReturn(viewCounts);

        // When
        scheduler.flushAllCounts();

        // Then - DB에 벌크 업데이트
        verify(postQueryRepository).bulkIncrementCount(eq(viewCounts), any(NumberPath.class));
        // JSON LIST 전체에 카운터 증분 (postId별로 incrementCounterInAllLists 호출)
        verify(redisPostListDeleteAdapter).incrementCounterInAllLists(eq(1L), eq("viewCount"), eq(5L));
        verify(redisPostListDeleteAdapter).incrementCounterInAllLists(eq(2L), eq("viewCount"), eq(3L));
    }

    @Test
    @DisplayName("조회수 DB 업데이트 실패 시 예외를 잡아 로깅")
    void shouldCatchViewCountException_whenDbUpdateFails() {
        // Given
        Map<Long, Long> viewCounts = Map.of(1L, 5L);
        given(redisPostViewAdapter.getAndClearViewCounts()).willReturn(viewCounts);
        doThrow(new RuntimeException("DB 오류")).when(postQueryRepository).bulkIncrementCount(any(), any());

        // When - 예외가 전파되지 않아야 함
        scheduler.flushAllCounts();

        // Then
        verify(postQueryRepository).bulkIncrementCount(eq(viewCounts), any(NumberPath.class));
    }
}
