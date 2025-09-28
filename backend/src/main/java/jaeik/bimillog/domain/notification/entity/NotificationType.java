package jaeik.bimillog.domain.notification.entity;

/**
 * <h2>알림 유형</h2>
 * <p>알림의 유형을 정의하는 열거형입니다.</p>
 * <p>롤링페이퍼 메시지, 댓글, 인기글 선정, 관리자 공지 분류</p>
 * <p>각 알림 유형은 NotificationUtilAdapter에서 사용자 설정 필드와 연결되어 수신 여부 제어</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum NotificationType {

    PAPER, // 롤링페이퍼에 메시지가 달렸을 때 (Setting.messageNotification과 연결)
    COMMENT, // 게시글에 댓글이 달렸을 때 (Setting.commentNotification과 연결)
    POST_FEATURED, // 주간, 전설 인기글이 되었을 때 (Setting.postFeaturedNotification과 연결)
    ADMIN, // 관리자 알림 (설정 연동 없음)
    INITIATE // SSE 초기화 용도 (설정 연동 없음)

}

