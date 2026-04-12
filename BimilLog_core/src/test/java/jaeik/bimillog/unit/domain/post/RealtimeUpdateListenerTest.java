package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.event.PostEvent.PostDetailViewedEvent;
import jaeik.bimillog.domain.post.event.PostLikedEvent;
import jaeik.bimillog.domain.post.event.PostEvent.PostUnlikedEvent;
import jaeik.bimillog.domain.post.listener.RealtimeUpdateListener;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>RealtimeUpdateListener 단위 테스트</h2>
 * <p>실시간 인기글 점수 리스너의 이벤트 처리 로직을 검증합니다.</p>
 */
@Tag("unit")
@DisplayName("RealtimeUpdateListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RealtimeUpdateListenerTest {

    @Mock
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @InjectMocks
    private RealtimeUpdateListener listener;

    @Test
    @DisplayName("댓글 작성 이벤트 - 점수 3점 증가")
    void handleCommentCreated_shouldIncrementScoreByThree() {
        // Given
        CommentCreatedEvent event = CommentCreatedEvent.of(1L, "작성자", 2L, 100L);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(100L, 3.0);
    }

    @Test
    @DisplayName("게시글 추천 이벤트 - 점수 4점 증가")
    void handlePostLiked_shouldIncrementScoreByFour() {
        // Given
        PostLikedEvent event = PostLikedEvent.of(1L, null, null);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, 4.0);
    }

    @Test
    @DisplayName("게시글 추천취소 이벤트 - 점수 4점 감소")
    void handlePostUnliked_shouldDecrementScoreByFour() {
        // Given
        PostUnlikedEvent event = new PostUnlikedEvent(1L);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, -4.0);
    }

    @Test
    @DisplayName("댓글 삭제 이벤트 - 점수 3점 감소")
    void handleCommentDeleted_shouldDecrementScoreByThree() {
        // Given
        CommentDeletedEvent event = new CommentDeletedEvent(100L);

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(100L, -3.0);
    }

    @Test
    @DisplayName("게시글 상세 조회 이벤트 - 점수 2점 증가")
    void handlePostDetailViewed_shouldIncrementScore() {
        // Given
        PostDetailViewedEvent event = new PostDetailViewedEvent(1L, "test-viewer");

        // When
        listener.handleRealtimeScore(event);

        // Then
        verify(redisPostRealTimeAdapter, times(1)).incrementRealtimePopularScore(1L, 2.0);
    }
}
