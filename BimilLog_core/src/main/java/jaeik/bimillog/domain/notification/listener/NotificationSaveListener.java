package jaeik.bimillog.domain.notification.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendEvent;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>알림 저장 이벤트 리스너</h2>
 * <p>다양한 도메인 이벤트를 수신하여 알림을 저장합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "알림 저장 이벤트")
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSaveListener {
    private final NotificationCommandService notificationCommandService;

    @Async("saveNotificationExecutor")
    @TransactionalEventListener(value = CommentCreatedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        notificationCommandService.saveCommentNotification(
                event.postUserId(),
                event.commenterName(),
                event.postId()
        );
    }

    @Async("saveNotificationExecutor")
    @TransactionalEventListener(value = RollingPaperEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handleRollingPaperEvent(RollingPaperEvent event) {
        notificationCommandService.saveMessageNotification(
                event.paperOwnerId(),
                event.memberName()
        );
    }

    @Async("saveNotificationExecutor")
    @TransactionalEventListener(value = PostFeaturedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handlePostFeaturedEvent(PostFeaturedEvent event) {
        notificationCommandService.savePopularNotification(
                event.memberId(),
                event.sseMessage(),
                event.postId(),
                event.notificationType(),
                event.postTitle()
        );
    }

    @Async("saveNotificationExecutor")
    @TransactionalEventListener(value = FriendEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}")
    )
    public void handleFriendEvent(FriendEvent event) {
        notificationCommandService.saveFriendNotification(
                event.getReceiveMemberId(),
                event.getSseMessage(),
                event.getSenderName()
        );
    }

    /**
     * <h3>댓글 알림 저장 최종 실패 복구</h3>
     *
     * @param e 발생한 예외
     * @param event 댓글 생성 이벤트
     */
    @Recover
    public void recoverHandleCommentCreatedEvent(Exception e, CommentCreatedEvent event) {
        log.error("댓글 알림 저장 최종 실패: postUserId={}, postId={}",
                event.postUserId(), event.postId(), e);
    }

    /**
     * <h3>롤링페이퍼 알림 저장 최종 실패 복구</h3>
     *
     * @param e 발생한 예외
     * @param event 롤링페이퍼 이벤트
     */
    @Recover
    public void recoverHandleRollingPaperEvent(Exception e, RollingPaperEvent event) {
        log.error("롤링페이퍼 알림 저장 최종 실패: paperOwnerId={}",
                event.paperOwnerId(), e);
    }

    /**
     * <h3>인기글 알림 저장 최종 실패 복구</h3>
     *
     * @param e 발생한 예외
     * @param event 인기글 선정 이벤트
     */
    @Recover
    public void recoverHandlePostFeaturedEvent(Exception e, PostFeaturedEvent event) {
        log.error("인기글 알림 저장 최종 실패: memberId={}, postId={}",
                event.memberId(), event.postId(), e);
    }

    /**
     * <h3>친구 알림 저장 최종 실패 복구</h3>
     *
     * @param e 발생한 예외
     * @param event 친구 이벤트
     */
    @Recover
    public void recoverHandleFriendEvent(Exception e, FriendEvent event) {
        log.error("친구 알림 저장 최종 실패: receiveMemberId={}",
                event.getReceiveMemberId(), e);
    }
}
