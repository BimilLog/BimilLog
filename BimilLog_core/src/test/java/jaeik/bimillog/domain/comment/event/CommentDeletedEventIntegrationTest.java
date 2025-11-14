package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>댓글 삭제 이벤트 워크플로우 통합 테스트</h2>
 * <p>댓글 삭제 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("댓글 삭제 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class CommentDeletedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private RedisPostUpdateAdapter redisPostUpdateAdapter;

    private static final double COMMENT_DELETE_SCORE = -3.0;

    @Test
    @DisplayName("댓글 삭제 이벤트 워크플로우 - 실시간 인기글 점수 감소")
    void commentDeletedEventWorkflow_ShouldCompleteScoreDecrement() {
        // Given
        var event = new CommentDeletedEvent(100L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(100L), eq(COMMENT_DELETE_SCORE));
        });
    }

    @Test
    @DisplayName("여러 다른 게시글의 댓글 삭제 이벤트 동시 처리")
    void multipleDifferentCommentDeletedEvents_ShouldProcessIndependently() {
        // Given
        var events = java.util.List.of(
                new CommentDeletedEvent(100L),
                new CommentDeletedEvent(101L),
                new CommentDeletedEvent(102L)
        );

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(100L), eq(COMMENT_DELETE_SCORE));
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(101L), eq(COMMENT_DELETE_SCORE));
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(102L), eq(COMMENT_DELETE_SCORE));
        });
    }

    @Test
    @DisplayName("동일 게시글의 여러 댓글 삭제 이벤트 처리")
    void multipleDeleteEventsForSamePost_ShouldProcessAll() {
        // Given
        var events = new java.util.ArrayList<CommentDeletedEvent>();
        for (int i = 0; i < 3; i++) {
            events.add(new CommentDeletedEvent(100L));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPostUpdateAdapter, times(3)).incrementRealtimePopularScore(eq(100L), eq(COMMENT_DELETE_SCORE));
        });
    }

    @Test
    @DisplayName("점수 감소 실패 시에도 시스템 정상 작동")
    void commentDeletedEventWithException_ShouldContinueWorking() {
        // Given
        var event = new CommentDeletedEvent(100L);

        // 점수 감소 실패 시뮬레이션 - 리스너가 예외를 catch하여 로그 처리
        doThrow(new RuntimeException("Redis 점수 감소 실패"))
                .when(redisPostUpdateAdapter).incrementRealtimePopularScore(100L, COMMENT_DELETE_SCORE);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(100L), eq(COMMENT_DELETE_SCORE));
        });
    }

}
