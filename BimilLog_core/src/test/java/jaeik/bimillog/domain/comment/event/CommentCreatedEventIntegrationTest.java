package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import jaeik.bimillog.testutil.BaseEventIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>댓글 생성 이벤트 워크플로우 통합 테스트</h2>
 * <p>댓글 생성 시 실시간 인기글 점수 증가를 검증하는 통합 테스트</p>
 * <p>CommentCreatedEvent 발생 시 비동기로 실시간 인기글 점수가 3점 증가하는지 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.3.0
 */
@DisplayName("댓글 생성 이벤트 워크플로우 통합 테스트")
@Tag("integration")
class CommentCreatedEventIntegrationTest extends BaseEventIntegrationTest {

    @MockitoBean
    private NotificationCommandService notificationCommandService;

    @MockitoBean
    private RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;

    @MockitoBean
    private RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;

    private static final double COMMENT_SCORE = 3.0;

    @Test
    @DisplayName("댓글 생성 이벤트 발생 시 실시간 인기글 점수 증가 검증")
    void commentCreatedEventWorkflow_ShouldIncrementScore() {
        // Given
        var event = new CommentCreatedEvent(testMember.getId(), testMember.getMemberName(), 1L, 100L);

        // When & Then
        publishAndVerify(event, () -> {
            // Redis 실시간 인기글 점수 증가 검증
            verify(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(
                    eq(100L), eq(COMMENT_SCORE));
        });
    }

    @Test
    @DisplayName("다중 댓글 생성 이벤트 처리 - 각 이벤트가 독립적으로 처리됨")
    void multipleCommentCreatedEvents_ShouldProcessIndependently() {
        // Given - 다양한 댓글 이벤트 생성
        var events = new java.util.ArrayList<CommentCreatedEvent>();
        events.add(new CommentCreatedEvent(testMember.getId(), "댓글작성자1", 1L, 100L));  // 동일 게시글
        events.add(new CommentCreatedEvent(testMember.getId(), "댓글작성자2", 1L, 100L));  // 동일 게시글
        events.add(new CommentCreatedEvent(otherMember.getId(), "댓글작성자3", 2L, 101L));  // 다른 게시글

        // When & Then - 각 이벤트가 모두 처리되는지 검증
        publishEvents(events);
        verifyAsyncSlow(() -> {
            // 실시간 인기글 점수 증가 검증
            verify(redisRealTimePostStoreAdapter, times(2)).incrementRealtimePopularScore(
                    eq(100L), eq(COMMENT_SCORE));
            verify(redisRealTimePostStoreAdapter).incrementRealtimePopularScore(
                    eq(101L), eq(COMMENT_SCORE));
        });
    }
}