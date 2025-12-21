package jaeik.bimillog.domain.notification.event;

import jaeik.bimillog.domain.notification.entity.NotificationType;

/**
 * <h2>알림 전송 이벤트</h2>
 * <p>SSE 및 FCM 알림 전송을 위한 통합 이벤트</p>
 * <p>FCM 메시지 조립은 FcmCommandService에서 처리</p>
 *
 * @param memberId 알림 수신자 ID
 * @param type 알림 타입
 * @param message SSE 메시지
 * @param url SSE 이동 URL
 * @param relatedMemberName 댓글 작성자 이름 또는 친구 요청 보낸 사람 이름 (COMMENT, FRIEND 타입만 사용, optional)
 * @param postTitle 게시글 제목 (POST_FEATURED 타입들만 사용, optional)
 * @author Jaeik
 * @since 2.0.0
 */
public record AlarmSendEvent(
        Long memberId,
        NotificationType type,
        String message,
        String url,
        String relatedMemberName,
        String postTitle
) {
    /**
     * COMMENT 타입용 생성자
     */
    public static AlarmSendEvent ofComment(Long memberId, String message, String url, String commenterName) {
        return new AlarmSendEvent(memberId, NotificationType.COMMENT, message, url, commenterName, null);
    }

    /**
     * FRIEND 타입용 생성자
     */
    public static AlarmSendEvent ofFriend(Long memberId, String message, String url, String senderName) {
        return new AlarmSendEvent(memberId, NotificationType.FRIEND, message, url, senderName, null);
    }

    /**
     * POST_FEATURED 타입용 생성자
     */
    public static AlarmSendEvent ofPostFeatured(Long memberId, NotificationType type, String message, String url, String postTitle) {
        return new AlarmSendEvent(memberId, type, message, url, null, postTitle);
    }

    /**
     * MESSAGE 타입용 생성자
     */
    public static AlarmSendEvent of(Long memberId, NotificationType type, String message, String url) {
        return new AlarmSendEvent(memberId, type, message, url, null, null);
    }
}
