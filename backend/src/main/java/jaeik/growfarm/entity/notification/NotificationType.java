package jaeik.growfarm.entity.notification;

/**
 * <h2>알림 타입</h2>
 * <p>다양한 알림의 종류를 정의하는 열거형</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
public enum NotificationType {
    ADMIN, // 관리자에게 받은 알림
    FARM, // 농장 관련 알림
    COMMENT, // 자신의 글에 댓글이 달렸을 때
    POST_FEATURED, // 자신의 글이 인기 글이 되었을 때
    COMMENT_FEATURED, // 자신의 댓글이 인기 댓글이 되었을 때
    INITIATE // SSE 연결 시작
}
