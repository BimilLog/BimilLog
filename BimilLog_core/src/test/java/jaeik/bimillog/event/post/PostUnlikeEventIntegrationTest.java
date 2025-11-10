package jaeik.bimillog.event.post;

import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import jaeik.bimillog.domain.post.event.PostUnlikeEvent;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 추천 취소 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 추천 취소 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("게시글 추천 취소 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class PostUnlikeEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private RedisPostUpdateAdapter redisPostUpdateAdapter;

    private static final double UNLIKE_SCORE = -4.0;

    @Test
    @DisplayName("게시글 추천 취소 이벤트 워크플로우 - 실시간 인기글 점수 감소")
    void postUnlikeEventWorkflow_ShouldCompleteScoreDecrement() {
        // Given
        var event = new PostUnlikeEvent(1L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(1L), eq(UNLIKE_SCORE));
        });
    }

    @Test
    @DisplayName("여러 다른 게시글 추천 취소 이벤트 동시 처리")
    void multipleDifferentPostUnlikeEvents_ShouldProcessIndependently() {
        // Given
        var events = java.util.List.of(
                new PostUnlikeEvent(1L),
                new PostUnlikeEvent(2L),
                new PostUnlikeEvent(3L)
        );

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(1L), eq(UNLIKE_SCORE));
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(2L), eq(UNLIKE_SCORE));
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(3L), eq(UNLIKE_SCORE));
        });
    }

    @Test
    @DisplayName("동일 게시글의 여러 추천 취소 이벤트 처리")
    void multipleUnlikeEventsForSamePost_ShouldProcessAll() {
        // Given
        var events = new java.util.ArrayList<PostUnlikeEvent>();
        for (int i = 0; i < 3; i++) {
            events.add(new PostUnlikeEvent(1L));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisPostUpdateAdapter, times(3)).incrementRealtimePopularScore(eq(1L), eq(UNLIKE_SCORE));
        });
    }

    @Test
    @DisplayName("점수 감소 실패 시에도 시스템 정상 작동")
    void postUnlikeEventWithException_ShouldContinueWorking() {
        // Given
        var event = new PostUnlikeEvent(1L);

        // 점수 감소 실패 시뮬레이션 - 리스너가 예외를 catch하여 로그 처리
        doThrow(new RuntimeException("Redis 점수 감소 실패"))
                .when(redisPostUpdateAdapter).incrementRealtimePopularScore(1L, UNLIKE_SCORE);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(redisPostUpdateAdapter).incrementRealtimePopularScore(eq(1L), eq(UNLIKE_SCORE));
        });
    }

}
