package jaeik.bimillog.domain.friend.listener;

import io.lettuce.core.RedisCommandTimeoutException;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentLikeEvent;
import jaeik.bimillog.domain.friend.service.FriendEventDlqService;
import jaeik.bimillog.domain.post.event.PostLikeEvent;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jaeik.bimillog.infrastructure.log.Log;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.INTERACTION_SCORE_DEFAULT;

/**
 * <h2>친구 상호작용 점수 관리 리스너</h2>
 * <p>게시글 좋아요, 댓글 작성, 댓글 좋아요 이벤트를 수신하여 Redis에 상호작용 점수를 기록합니다.</p>
 * <p>친구 추천 알고리즘에서 사용되는 상호작용 점수를 실시간으로 업데이트합니다.</p>
 * <p>익명 사용자의 상호작용은 점수에 반영되지 않습니다.</p>
 * <p>각 상호작용당 +0.5점, 최대 9.5점까지 증가합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "친구 상호작용 점수")
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendInteractionListener {
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final FriendEventDlqService friendEventDlqService;

    /**
     * <h3>게시글 좋아요 상호작용 점수 증가</h3>
     * <p>게시글 좋아요 시 게시글 작성자와 좋아요 누른 사람 간의 상호작용 점수를 증가시킵니다.</p>
     * <p>익명 게시글의 경우 점수가 증가하지 않습니다.</p>
     * <p>{@link PostLikeEvent} 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 게시글 좋아요 이벤트
     */
    @EventListener
    @Async("realtimeEventExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}"))
    public void handlePostLiked(PostLikeEvent event) {
        boolean processed = redisInteractionScoreRepository.addInteractionScore(event.getPostAuthorId(), event.getLikerId(), event.getEventId());
        if (!processed) {
            log.info("이미 처리된 게시글 좋아요 이벤트 : postId={}, idempotencyKey={}", event.getPostId(), event.getEventId());
        }
    }

    @Recover
    public void recoverPostLiked(Exception e, PostLikeEvent event) {
        log.warn("게시글 추천 상호작용 점수 증가 실패 DLQ 진입: postId={}, authorId={}, likerId={}", event.getPostId(), event.getPostAuthorId(), event.getLikerId(), e);
        friendEventDlqService.saveScoreUp(event.getEventId(), event.getPostAuthorId(), event.getLikerId(), INTERACTION_SCORE_DEFAULT);
    }

    /**
     * <h3>댓글 작성 상호작용 점수 증가</h3>
     * <p>댓글 작성 시 게시글 작성자와 댓글 작성자 간의 상호작용 점수를 증가시킵니다.</p>
     * <p>익명 댓글의 경우 점수가 증가하지 않습니다.</p>
     * <p>{@link CommentCreatedEvent} 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 댓글 작성 이벤트
     */
    @EventListener
    @Async("realtimeEventExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handleCommentCreated(CommentCreatedEvent event) {
        // 익명 댓글은 상호작용 점수에 반영하지 않음
        if (event.commenterId() == null) {
            log.debug("익명 댓글 작성은 상호작용 점수에 반영되지 않습니다: postId={}", event.postId());
            return;
        }

        // 자기 자신의 게시글에 댓글 작성한 경우 제외
        if (event.postUserId().equals(event.commenterId())) {
            log.debug("자기 자신의 게시글에 댓글 작성은 상호작용 점수에 반영되지 않습니다: postId={}", event.postId());
            return;
        }

        boolean processed = redisInteractionScoreRepository.addInteractionScore(
                event.postUserId(), event.commenterId(), event.getIdempotencyKey());

        if (processed) {
            log.debug("댓글 작성 상호작용 점수 증가: postId={}, postUserId={}, commenterId={}",
                    event.postId(), event.postUserId(), event.commenterId());
        } else {
            log.debug("이미 처리된 댓글 작성 이벤트 (멱등성 스킵): postId={}, idempotencyKey={}",
                    event.postId(), event.getIdempotencyKey());
        }
    }

    @Recover
    public void recoverCommentCreated(Exception e, CommentCreatedEvent event) {
        log.error("댓글 작성 상호작용 점수 증가 최종 실패: postId={}, postUserId={}, commenterId={}",
                event.postId(), event.postUserId(), event.commenterId(), e);

        if (event.commenterId() != null && !event.postUserId().equals(event.commenterId())) {
            friendEventDlqService.saveScoreUp(event.getIdempotencyKey(), event.postUserId(), event.commenterId(), INTERACTION_SCORE_DEFAULT);
        }
    }

    /**
     * <h3>댓글 좋아요 상호작용 점수 증가</h3>
     * <p>댓글 좋아요 시 댓글 작성자와 좋아요 누른 사람 간의 상호작용 점수를 증가시킵니다.</p>
     * <p>익명 댓글의 경우 점수가 증가하지 않습니다.</p>
     * <p>{@link CommentLikeEvent} 발행 시 비동기로 호출됩니다.</p>
     *
     * @param event 댓글 좋아요 이벤트
     */
    @EventListener
    @Async("realtimeEventExecutor")
    @Retryable(
            retryFor = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class,
                    RedisCommandTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handleCommentLiked(CommentLikeEvent event) {
        // 익명 댓글은 상호작용 점수에 반영하지 않음
        if (event.commentAuthorId() == null) {
            log.debug("익명 댓글 좋아요는 상호작용 점수에 반영되지 않습니다: commentId={}", event.commentId());
            return;
        }

        // 자기 자신의 댓글에 좋아요한 경우 제외 (이미 블랙리스트 체크로 방지됨)
        if (event.commentAuthorId().equals(event.likerId())) {
            log.debug("자기 자신의 댓글 좋아요는 상호작용 점수에 반영되지 않습니다: commentId={}", event.commentId());
            return;
        }

        boolean processed = redisInteractionScoreRepository.addInteractionScore(
                event.commentAuthorId(), event.likerId(), event.getIdempotencyKey());

        if (processed) {
            log.debug("댓글 좋아요 상호작용 점수 증가: commentId={}, authorId={}, likerId={}",
                    event.commentId(), event.commentAuthorId(), event.likerId());
        } else {
            log.debug("이미 처리된 댓글 좋아요 이벤트 (멱등성 스킵): commentId={}, idempotencyKey={}",
                    event.commentId(), event.getIdempotencyKey());
        }
    }

    @Recover
    public void recoverCommentLiked(Exception e, CommentLikeEvent event) {
        log.error("댓글 좋아요 상호작용 점수 증가 최종 실패: commentId={}, authorId={}, likerId={}",
                event.commentId(), event.commentAuthorId(), event.likerId(), e);

        if (event.commentAuthorId() != null && !event.commentAuthorId().equals(event.likerId())) {
            friendEventDlqService.saveScoreUp(event.getIdempotencyKey(), event.commentAuthorId(), event.likerId(), INTERACTION_SCORE_DEFAULT);
        }
    }
}
