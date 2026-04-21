package jaeik.bimillog.domain.notification.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendEvent.FriendRequestEvent;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.event.AlarmSendEvent;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.service.NotificationEventCallback;
import jaeik.bimillog.domain.paper.event.PaperEvent.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostEvent.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * <p>URL/메시지 조립 후 {@link NotificationCommandService#saveNotification} 템플릿 호출.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, message = "알림 저장 이벤트")
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSaveListener {
    private final NotificationCommandService notificationCommandService;

    @Value("${url}")
    private String baseUrl;

    @Async("saveNotificationExecutor")
    @TransactionalEventListener(value = CommentCreatedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        if (event.postUserId() == null || event.postUserId().equals(event.commenterId())) {
            return;
        }
        final String message = NotificationType.COMMENT.buildSseMessage(event.commenterName());
        final String url = NotificationType.COMMENT.buildUrl(baseUrl, event.postId());

        notificationCommandService.saveNotification(
                event.postUserId(),
                NotificationType.COMMENT,
                message,
                url,
                member -> AlarmSendEvent.ofComment(member.getId(), message, url, event.commenterName())
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
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void handleRollingPaperEvent(RollingPaperEvent event) {
        final String message = NotificationType.MESSAGE.buildSseMessage();
        final String url = NotificationType.MESSAGE.buildUrl(baseUrl, event.memberName());

        notificationCommandService.saveNotification(
                event.paperOwnerId(),
                NotificationType.MESSAGE,
                message,
                url,
                member -> AlarmSendEvent.of(member.getId(), NotificationType.MESSAGE, message, url)
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
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void handlePostFeaturedEvent(PostFeaturedEvent event) {
        final String message = event.sseMessage();
        final String url = event.notificationType().buildUrl(baseUrl, event.postId());

        notificationCommandService.saveNotification(
                event.memberId(),
                event.notificationType(),
                message,
                url,
                member -> AlarmSendEvent.ofPostFeatured(member.getId(), event.notificationType(), message, url, event.postTitle())
        );
    }

    @Async("saveNotificationExecutor")
    @TransactionalEventListener(value = FriendRequestEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    QueryTimeoutException.class
            },
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void handleFriendEvent(FriendRequestEvent event) {
        final String message = event.sseMessage();
        final String url = NotificationType.FRIEND.buildUrl(baseUrl);

        notificationCommandService.saveNotification(
                event.receiveMemberId(),
                NotificationType.FRIEND,
                message,
                url,
                member -> AlarmSendEvent.ofFriend(member.getId(), message, url, event.senderName())
        );
    }

    @Recover
    public void recoverHandleCommentCreatedEvent(Exception e, CommentCreatedEvent event) {
        log.error("댓글 알림 저장 최종 실패: postUserId={}, postId={}", event.postUserId(), event.postId(), e);
    }

    @Recover
    public void recoverHandleRollingPaperEvent(Exception e, RollingPaperEvent event) {
        log.error("롤링페이퍼 알림 저장 최종 실패: paperOwnerId={}", event.paperOwnerId(), e);
    }

    @Recover
    public void recoverHandlePostFeaturedEvent(Exception e, PostFeaturedEvent event) {
        log.error("인기글 알림 저장 최종 실패: memberId={}, postId={}", event.memberId(), event.postId(), e);
    }

    @Recover
    public void recoverHandleFriendEvent(Exception e, FriendRequestEvent event) {
        log.error("친구 알림 저장 최종 실패: receiveMemberId={}", event.receiveMemberId(), e);
    }
}
