package jaeik.bimillog.domain.post.async;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <h2>실시간 인기글 점수 업데이트</h2>
 * <p>게시글 조회, 댓글 작성, 추천 이벤트를 수신하여 실시간 인기글 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 댓글: +3점/-3점, 추천: +4점/-4점</p>
 * <p>비동기 처리를 통해 이벤트 발행자와 독립적으로 실행됩니다.</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "실시간 인기글 점수")
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimePostSync {
    private final RedisRealTimePostAdapter redisRealTimePostAdapter;

    private static final double COMMENT_SCORE = 3.0;

    /**
     * <h3>실시간 인기글 점수 업데이트</h3>
     * <p>게시글의 실시간 인기글 점수를 주어진 값만큼 증감시킵니다.</p>
     *
     * @param postId 게시글 ID
     * @param score 증감할 점수 (양수: 증가, 음수: 감소)
     */
    @Async("realtimeEventExecutor")
    public void updateRealtimeScore(Long postId, double score) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(postId, score);
    }

    /**
     * <h3>댓글 작성 이벤트 처리</h3>
     * <p>댓글 작성 시 해당 게시글의 실시간 인기글 점수를 3점 증가시킵니다.</p>
     *
     * @param event 댓글 작성 이벤트
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentCreated(CommentCreatedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.getPostId(), COMMENT_SCORE);
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     * <p>댓글 삭제 시 해당 게시글의 실시간 인기글 점수를 3점 감소시킵니다.</p>
     *
     * @param event 댓글 삭제 이벤트
     */
    @TransactionalEventListener
    @Async("realtimeEventExecutor")
    public void handleCommentDeleted(CommentDeletedEvent event) {
        redisRealTimePostAdapter.incrementRealtimePopularScore(event.postId(), -COMMENT_SCORE);
    }
}
