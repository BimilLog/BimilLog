package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.infrastructure.redis.post.RedisDetailPostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>게시글 추천 이벤트 워크플로우 통합 테스트</h2>
 * <p>게시글 추천 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>비동기 이벤트 처리와 실제 스프링 컨텍스트를 사용하여 전체 워크플로우를 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("게시글 추천 이벤트 워크플로우 통합 테스트")
@Tag("integration")
public class PostLikeEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private RedisDetailPostStoreAdapter redisDetailPostStoreAdapter;

    @MockitoBean
    private RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;

    private static final double LIKE_SCORE = 4.0;

    @Test
    @DisplayName("게시글 추천 이벤트 워크플로우 - 실시간 인기글 점수 증가")
    void postLikeEventWorkflow_ShouldCompleteScoreIncrement() {
        // Given
        var event = new PostLikeEvent(1L, 1L, 1L);

        // When & Then
        publishAndVerify(event, () -> {
            verify(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(eq(1L), eq(LIKE_SCORE));
            verify(redisDetailPostStoreAdapter).deleteSinglePostCache(eq(1L));
        });
    }

    @Test
    @DisplayName("여러 다른 게시글 추천 이벤트 동시 처리")
    void multipleDifferentPostLikeEvents_ShouldProcessIndependently() {
        // Given
        var events = java.util.List.of(
                new PostLikeEvent(1L, 1L, 1L),
                new PostLikeEvent(2L, 2L, 2L),
                new PostLikeEvent(3L, 3L, 3L)
        );

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(eq(1L), eq(LIKE_SCORE));
            verify(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(eq(2L), eq(LIKE_SCORE));
            verify(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(eq(3L), eq(LIKE_SCORE));
            verify(redisDetailPostStoreAdapter).deleteSinglePostCache(eq(1L));
            verify(redisDetailPostStoreAdapter).deleteSinglePostCache(eq(2L));
            verify(redisDetailPostStoreAdapter).deleteSinglePostCache(eq(3L));
        });
    }

    @Test
    @DisplayName("동일 게시글의 여러 추천 이벤트 처리")
    void multipleLikeEventsForSamePost_ShouldProcessAll() {
        // Given
        var events = new java.util.ArrayList<PostLikeEvent>();
        for (int i = 0; i < 3; i++) {
            events.add(new PostLikeEvent(1L, 1L, 1L));
        }

        // When & Then
        publishEvents(events);
        verifyAsync(() -> {
            verify(redisRealTimePostStoreAdapter, times(3)).incrementRealtimePopularScore(eq(1L), eq(LIKE_SCORE));
            verify(redisDetailPostStoreAdapter, times(3)).deleteSinglePostCache(eq(1L));
        });
    }

    @Test
    @DisplayName("점수 증가 실패 시에도 시스템 정상 작동")
    void postLikeEventWithException_ShouldContinueWorking() {
        // Given
        var event = new PostLikeEvent(1L, 1L, 1L);

        // 점수 증가 실패 시뮬레이션 - 리스너가 예외를 catch하여 로그 처리
        doThrow(new RuntimeException("Redis 점수 증가 실패"))
                .when(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(1L, LIKE_SCORE);

        // When & Then - 예외가 발생해도 시스템은 정상 작동
        publishAndVerify(event, () -> {
            verify(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(eq(1L), eq(LIKE_SCORE));
        });
    }

}
