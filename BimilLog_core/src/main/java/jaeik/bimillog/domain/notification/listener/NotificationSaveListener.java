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
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.Backoff;
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
                    RedisConnectionFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
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
                    RedisConnectionFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
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
                    RedisConnectionFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
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
                    RedisConnectionFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleFriendEvent(FriendEvent event) {
        notificationCommandService.saveFriendNotification(
                event.getReceiveMemberId(),
                event.getSseMessage(),
                event.getSenderName()
        );
    }
}
