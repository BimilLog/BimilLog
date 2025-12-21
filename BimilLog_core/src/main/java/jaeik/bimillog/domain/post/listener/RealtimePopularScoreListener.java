package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.domain.post.event.PostUnlikeEvent;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.infrastructure.redis.post.RedisPostDeleteAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisPostUpdateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>실시간 인기글 점수 업데이트 리스너</h2>
 * <p>게시글 조회, 댓글 작성, 추천 이벤트를 수신하여 실시간 인기글 점수를 업데이트합니다.</p>
 * <p>조회: +2점, 댓글: +3점, 추천: +4점</p>
 * <p>비동기 처리를 통해 이벤트 발행자와 독립적으로 실행됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimePopularScoreListener {
    private final RedisPostUpdateAdapter redisPostUpdateAdapter;
    private final RedisPostDeleteAdapter redisPostDeleteAdapter;

    private static final double VIEW_SCORE = 2.0;
    private static final double COMMENT_SCORE = 3.0;
    private static final double LIKE_SCORE = 4.0;

    /**
     * <h3>게시글 조회 이벤트 처리</h3>
     * <p>게시글 조회 시 실시간 인기글 점수를 2점 증가시킵니다.</p>
     * <p>PostViewedEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 게시글 조회 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handlePostViewed(PostViewedEvent event) {
        try {
            redisPostUpdateAdapter.incrementRealtimePopularScore(event.postId(), VIEW_SCORE);
            log.debug("실시간 인기글 점수 증가 (조회): postId={}, score=+{}", event.postId(), VIEW_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기글 점수 증가 실패 (조회): postId={}", event.postId(), e);
        }
    }

    /**
     * <h3>댓글 작성 이벤트 처리</h3>
     * <p>댓글 작성 시 해당 게시글의 실시간 인기글 점수를 3점 증가시킵니다.</p>
     * <p>CommentCreatedEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 댓글 작성 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handleCommentCreated(CommentCreatedEvent event) {
        try {
            redisPostUpdateAdapter.incrementRealtimePopularScore(event.postId(), COMMENT_SCORE);
            log.debug("실시간 인기글 점수 증가 (댓글): postId={}, score=+{}", event.postId(), COMMENT_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기글 점수 증가 실패 (댓글): postId={}", event.postId(), e);
        }
    }

    /**
     * <h3>게시글 추천 이벤트 처리</h3>
     * <p>게시글 추천 시 실시간 인기글 점수를 4점 증가시키고 캐시를 무효화합니다.</p>
     * <p>PostLikeEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 게시글 추천 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handlePostLiked(PostLikeEvent event) {
        try {
            redisPostUpdateAdapter.incrementRealtimePopularScore(event.postId(), LIKE_SCORE);
            redisPostDeleteAdapter.deleteSinglePostCache(event.postId());
            log.debug("실시간 인기글 점수 증가 및 캐시 무효화 (추천): postId={}, score=+{}", event.postId(), LIKE_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기글 점수 증가 실패 (추천): postId={}", event.postId(), e);
        }
    }

    /**
     * <h3>게시글 추천 취소 이벤트 처리</h3>
     * <p>게시글 추천 취소 시 실시간 인기글 점수를 4점 감소시키고 캐시를 무효화합니다.</p>
     * <p>PostUnlikeEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 게시글 추천 취소 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handlePostUnliked(PostUnlikeEvent event) {
        try {
            redisPostUpdateAdapter.incrementRealtimePopularScore(event.postId(), -LIKE_SCORE);
            redisPostDeleteAdapter.deleteSinglePostCache(event.postId());
            log.debug("실시간 인기글 점수 감소 및 캐시 무효화 (추천 취소): postId={}, score=-{}", event.postId(), LIKE_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기글 점수 감소 실패 (추천 취소): postId={}", event.postId(), e);
        }
    }

    /**
     * <h3>댓글 삭제 이벤트 처리</h3>
     * <p>댓글 삭제 시 해당 게시글의 실시간 인기글 점수를 3점 감소시킵니다.</p>
     * <p>CommentDeletedEvent 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 댓글 삭제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handleCommentDeleted(CommentDeletedEvent event) {
        try {
            redisPostUpdateAdapter.incrementRealtimePopularScore(event.postId(), -COMMENT_SCORE);
            log.debug("실시간 인기글 점수 감소 (댓글 삭제): postId={}, score=-{}", event.postId(), COMMENT_SCORE);
        } catch (Exception e) {
            log.error("실시간 인기글 점수 감소 실패 (댓글 삭제): postId={}", event.postId(), e);
        }
    }
}
