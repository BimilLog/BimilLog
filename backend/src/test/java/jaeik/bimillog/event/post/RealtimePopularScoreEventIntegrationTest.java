package jaeik.bimillog.event.post;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>실시간 인기글 점수 이벤트 워크플로우 통합 테스트</h2>
 * <p>조회/댓글/추천 이벤트 발생 시 실시간 인기글 점수가 올바르게 증가하는지 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("실시간 인기글 점수 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class RealtimePopularScoreEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private RedisPostCommandPort redisPostCommandPort;

    private static final double VIEW_SCORE = 2.0;
    private static final double COMMENT_SCORE = 3.0;
    private static final double LIKE_SCORE = 4.0;

    @Test
    @DisplayName("게시글 조회 이벤트 워크플로우 - 실시간 인기글 점수 +2점")
    void postViewedEvent_ShouldIncrementScoreByTwo() {
        // Given
        var event = new PostViewedEvent(1L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(VIEW_SCORE));
            verifyNoMoreInteractions(redisPostCommandPort);
        });
    }

    @Test
    @DisplayName("댓글 작성 이벤트 워크플로우 - 실시간 인기글 점수 +3점")
    void commentCreatedEvent_ShouldIncrementScoreByThree() {
        // Given
        var event = new CommentCreatedEvent(100L, "작성자", 1L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(COMMENT_SCORE));
            verifyNoMoreInteractions(redisPostCommandPort);
        });
    }

    @Test
    @DisplayName("게시글 추천 이벤트 워크플로우 - 실시간 인기글 점수 +4점")
    void postLikeEvent_ShouldIncrementScoreByFour() {
        // Given
        var event = new PostLikeEvent(1L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(LIKE_SCORE));
            verifyNoMoreInteractions(redisPostCommandPort);
        });
    }

    @Test
    @DisplayName("여러 다른 이벤트 동시 처리 - 각각 올바른 점수 증가")
    void multipleDifferentEvents_ShouldProcessIndependently() {
        // Given
        var viewEvent = new PostViewedEvent(1L);
        var commentEvent = new CommentCreatedEvent(200L, "작성자", 2L);
        var likeEvent = new PostLikeEvent(3L);

        // When & Then
        publishEventsAndVerify(new Object[]{viewEvent, commentEvent, likeEvent}, () -> {
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(VIEW_SCORE));
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(2L), eq(COMMENT_SCORE));
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(3L), eq(LIKE_SCORE));
            verifyNoMoreInteractions(redisPostCommandPort);
        });
    }

    @Test
    @DisplayName("동일 게시글의 여러 이벤트 처리 - 누적 점수 증가")
    void multipleEventsForSamePost_ShouldAccumulateScores() {
        // Given
        var viewEvent = new PostViewedEvent(1L);
        var commentEvent = new CommentCreatedEvent(100L, "작성자", 1L);
        var likeEvent = new PostLikeEvent(1L);

        // When & Then
        publishEventsAndVerify(new Object[]{viewEvent, commentEvent, likeEvent}, () -> {
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(VIEW_SCORE));
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(COMMENT_SCORE));
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(LIKE_SCORE));
        });
    }

    @Test
    @DisplayName("점수 증가 실패 시에도 시스템 정상 작동")
    void scoreIncrementFailure_ShouldNotBreakSystem() {
        // Given
        var event = new PostViewedEvent(1L);

        doThrow(new RuntimeException("Redis 쓰기 실패"))
                .when(redisPostCommandPort).incrementRealtimePopularScore(1L, VIEW_SCORE);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(redisPostCommandPort).incrementRealtimePopularScore(eq(1L), eq(VIEW_SCORE));
            verifyNoMoreInteractions(redisPostCommandPort);
        });
    }
}
