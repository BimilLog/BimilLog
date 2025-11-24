package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.domain.notification.listener.NotificationGenerateListener;

/**
 * <h2>댓글 작성 이벤트</h2>
 * <p>댓글이 작성되었을 때 발생하는 이벤트</p>
 * <p>SSE와 FCM 알림을 트리거하고 상호작용 점수를 증가시킵니다</p>
 *
 * @param postUserId 게시글 작성자 ID (알림을 받을 사용자)
 * @param commenterName 댓글 작성자 이름
 * @param commenterId 댓글 작성자 ID (익명인 경우 null, 상호작용 점수 증가에 사용)
 * @param postId 게시글 ID
 * @author Jaeik
 * @version 2.0.0
 * {@link NotificationGenerateListener} SSE/FCM 알림 발송
 */
public record CommentCreatedEvent(
        Long postUserId,
        String commenterName,
        Long commenterId,
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
        // commenterId는 익명 댓글의 경우 null일 수 있음
    }
}