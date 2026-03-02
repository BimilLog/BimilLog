package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.domain.global.event.CacheCountEvent;
import jaeik.bimillog.domain.global.event.FriendInteractionEvent;
import jaeik.bimillog.domain.global.event.RealtimeScoreEvent;
import jaeik.bimillog.domain.notification.listener.NotificationSendListener;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * <h2>댓글 작성 이벤트</h2>
 * <p>댓글이 작성되었을 때 발생하는 이벤트</p>
 * <p>SSE와 FCM 알림을 트리거하고 상호작용 점수를 증가시킵니다</p>
 * <p>이때 게시글 작성자는 익명이면 안됨</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * {@link NotificationSendListener} SSE/FCM 알림 발송
 */
@Slf4j
public record CommentCreatedEvent(String eventId, Long postUserId, String commenterName, Long commenterId, Long postId)
        implements FriendInteractionEvent, RealtimeScoreEvent, CacheCountEvent {

    public static CommentCreatedEvent of(Long postUserId, String commenterName, Long commenterId, Long postId) {
        return new CommentCreatedEvent(
                UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                postUserId, commenterName, commenterId, postId
        );
    }

    @Override
    public double realtimeScore() { return 3.0; }

    @Override
    public String counterField() { return "commentCount"; }

    @Override
    public int counterDelta() { return 1; }

    @Override
    public Long getMemberId() { return commenterId; }

    @Override
    public Long getTargetMemberId() { return postUserId; }

    @Override
    public String getIdempotencyKey() { return eventId; }

    @Override
    public void getAlreadyProcess() {
        log.info("이미 처리된 댓글 작성 이벤트: postId={}, idempotencyKey={}", postId, eventId);
    }

    @Override
    public void getDlqMessage(Exception e) {
        log.warn("댓글 작성 상호작용 점수 증가 실패 DLQ 진입: postId={}, postUserId={}, commenterId={}", postId, postUserId, commenterId, e);
    }
}
