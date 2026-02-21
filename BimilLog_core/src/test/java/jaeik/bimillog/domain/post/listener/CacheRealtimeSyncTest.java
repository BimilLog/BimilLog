package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.async.CacheRealtimeSync;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostViewAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import jaeik.bimillog.domain.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * <h2>CacheRealtimeSync 단위 테스트</h2>
 * <p>실시간 인기글 점수 리스너의 이벤트 처리 로직을 검증합니다.</p>
 * <p>서킷브레이커 동작은 RedisRealTimePostAdapter에서 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Tag("unit")
@DisplayName("CacheRealtimeSync 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CacheRealtimeSyncTest {

    @Mock
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @Mock
    private RedisPostListUpdateAdapter redisPostListUpdateAdapter;

    @Mock
    private RedisPostViewAdapter redisPostViewAdapter;

    @Mock
    private PostRepository postRepository;

    private CacheRealtimeSync listener;

    @BeforeEach
    void setUp() {
        listener = new CacheRealtimeSync(
                redisPostRealTimeAdapter,
                redisPostListUpdateAdapter,
                redisPostViewAdapter,
                postRepository
        );
        reset(redisPostRealTimeAdapter);
    }

    @Test
    @DisplayName("댓글 작성 이벤트 - 점수 3점 증가")
    void handleCommentCreated_shouldIncrementScoreByThree() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);

        // When
        listener.handleCommentCreated(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(100L, 3.0);
    }

    @Test
    @DisplayName("실시간 점수 증가 - 양수 점수")
    void updateRealtimeScore_shouldIncrementScore() {
        // When
        listener.updateRealtimeScore(1L, 4.0);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, 4.0);
    }

    @Test
    @DisplayName("실시간 점수 감소 - 음수 점수")
    void updateRealtimeScore_shouldDecrementScore() {
        // When
        listener.updateRealtimeScore(1L, -4.0);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, -4.0);
    }

    @Test
    @DisplayName("댓글 삭제 이벤트 - 점수 3점 감소")
    void handleCommentDeleted_shouldDecrementScoreByThree() {
        // Given
        CommentDeletedEvent event = new CommentDeletedEvent(100L);

        // When
        listener.handleCommentDeleted(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(100L, -3.0);
    }
}
