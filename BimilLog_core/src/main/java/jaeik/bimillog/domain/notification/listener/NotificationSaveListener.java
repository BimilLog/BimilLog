package jaeik.bimillog.domain.notification.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendEvent;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSaveListener {
    private final NotificationCommandService notificationCommandService;

    @TransactionalEventListener(value = CommentCreatedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        notificationCommandService.saveCommentNotification(
                event.postUserId(),
                event.commenterName(),
                event.postId()
        );
    }

    @TransactionalEventListener(value = RollingPaperEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void handleRollingPaperEvent(RollingPaperEvent event) {
        notificationCommandService.saveMessageNotification(
                event.paperOwnerId(),
                event.memberName()
        );
    }

    @TransactionalEventListener(value = PostFeaturedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostFeaturedEvent(PostFeaturedEvent event) {
        notificationCommandService.savePopularNotification(
                event.memberId(),
                event.sseMessage(),
                event.postId(),
                event.notificationType(),
                event.postTitle()
        );
    }

    @TransactionalEventListener(value = FriendEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendEvent(FriendEvent event) {
        notificationCommandService.saveFriendNotification(
                event.getReceiveMemberId(),
                event.getSseMessage(),
                event.getSenderName()
        );
    }
}
