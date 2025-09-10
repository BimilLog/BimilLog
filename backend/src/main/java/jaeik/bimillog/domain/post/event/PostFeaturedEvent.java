package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.infrastructure.adapter.notification.in.listener.NotificationGenerateListener;

/**
 * <h2>인기글 등극 이벤트</h2>
 * <p>게시글이 주간 인기글, 전설의 게시글, 공지사항으로 등극했을 때 발생하는 비동기 이벤트</p>
 * <p>PostCacheController에서 인기글 캐시 업데이트 시 게시글의 PostCacheFlag가 설정될 때 발생합니다.</p>
 * <p>NotificationEventListener에서 수신하여 SSE와 FCM 알림을 비동기로 발송합니다.</p>
 * <p>작성자에게 자신의 게시글이 인기글이 되었음을 알리는 성취 알림 역할을 합니다.</p>
 *
 * @param userId 게시글 작성자 ID (알림을 받을 대상 사용자)
 * @param sseMessage SSE 알림 메시지 (브라우저 실시간 알림용)
 * @param postId 등극한 게시글 ID
 * @param fcmTitle FCM 푸시 알림 제목
 * @param fcmBody FCM 푸시 알림 내용
 * @author Jaeik
 * @version 2.0.0
 * {@link NotificationGenerateListener} 인기글 등극 알림 발송
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