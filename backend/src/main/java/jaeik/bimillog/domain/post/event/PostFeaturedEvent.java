package jaeik.bimillog.domain.post.event;

/**
 * <h2>인기글 등극 이벤트</h2>
 * <p>게시글이 주간 인기글이나 명예의 전당에 등극했을 때 발생하는 이벤트</p>
 * <p>SSE와 FCM 알림을 트리거합니다</p>
 *
 * @param userId 게시글 작성자 ID (알림을 받을 사용자)
 * @param sseMessage SSE 알림 메시지
 * @param postId 게시글 ID
 * @param fcmTitle FCM 알림 제목
 * @param fcmBody FCM 알림 내용
 * @author Jaeik
 * @version 2.0.0
 */
public record PostFeaturedEvent(
        Long userId,
        String sseMessage,
        Long postId,
        String fcmTitle,
        String fcmBody
) {
    public PostFeaturedEvent {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
        if (sseMessage == null || sseMessage.isBlank()) {
            throw new IllegalArgumentException("SSE 메시지는 null이거나 비어있을 수 없습니다.");
        }
        if (fcmTitle == null || fcmTitle.isBlank()) {
            throw new IllegalArgumentException("FCM 제목은 null이거나 비어있을 수 없습니다.");
        }
        if (fcmBody == null || fcmBody.isBlank()) {
            throw new IllegalArgumentException("FCM 내용은 null이거나 비어있을 수 없습니다.");
        }
    }
}