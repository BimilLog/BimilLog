package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.infrastructure.adapter.notification.in.listener.NotificationGenerateListener;

/**
 * <h2>댓글 작성 이벤트</h2>
 * <p>댓글이 작성되었을 때 발생하는 이벤트</p>
 * <p>SSE와 FCM 알림을 트리거합니다</p>
 *
 * @param postUserId 게시글 작성자 ID (알림을 받을 사용자)
 * @param commenterName 댓글 작성자 이름
 * @param postId 게시글 ID
 * @see NotificationGenerateListener SSE/FCM 알림 발송
 * @author Jaeik
 * @version 2.0.0
 */
public record CommentCreatedEvent(
        Long postUserId,
        String commenterName,
        Long postId
) {
    public CommentCreatedEvent {
        if (postUserId == null) {
            throw new IllegalArgumentException("게시글 작성자 ID는 null일 수 없습니다.");
        }
        if (commenterName == null || commenterName.isBlank()) {
            throw new IllegalArgumentException("댓글 작성자 이름은 null이거나 비어있을 수 없습니다.");
        }
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
    }
}