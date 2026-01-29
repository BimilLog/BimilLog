package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.event.PostUnlikeEvent;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * <h2>RealtimePopularScoreListener 단위 테스트</h2>
 * <p>실시간 인기글 점수 리스너의 이벤트 처리 로직을 검증합니다.</p>
 * <p>서킷브레이커 동작은 RedisRealTimePostAdapter에서 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Tag("unit")
@DisplayName("RealtimePopularScoreListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RealtimePopularScoreListenerTest {

    @Mock
    private RedisRealTimePostAdapter redisRealTimePostAdapter;

    @InjectMocks
    private RealtimePopularScoreListener listener;

    @BeforeEach
    void setUp() {
        reset(redisRealTimePostAdapter);
    }

    @Test
    @DisplayName("댓글 작성 이벤트 - 점수 3점 증가")
    void handleCommentCreated_shouldIncrementScoreByThree() {
        // Given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, "작성자", 2L, 100L);

        // When
        listener.handleCommentCreated(event);

        // Then
        verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(100L, 3.0);
    }

    @Test
    @DisplayName("게시글 추천 이벤트 - 점수 4점 증가")
    void handlePostLiked_shouldIncrementScoreByFour() {
        // Given
        PostLikeEvent event = new PostLikeEvent(1L, 2L, 3L);

        // When
        listener.handlePostLiked(event);

        // Then
        verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(1L, 4.0);
    }

    @Test
    @DisplayName("게시글 추천 취소 이벤트 - 점수 4점 감소")
    void handlePostUnliked_shouldDecrementScoreByFour() {
        // Given
        PostUnlikeEvent event = new PostUnlikeEvent(1L);

        // When
        listener.handlePostUnliked(event);

        // Then
        verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(1L, -4.0);
    }

    @Test
    @DisplayName("댓글 삭제 이벤트 - 점수 3점 감소")
    void handleCommentDeleted_shouldDecrementScoreByThree() {
        // Given
        CommentDeletedEvent event = new CommentDeletedEvent(100L);

        // When
        listener.handleCommentDeleted(event);

        // Then
        verify(redisRealTimePostAdapter, times(1)).incrementRealtimePopularScore(100L, -3.0);
    }
}
