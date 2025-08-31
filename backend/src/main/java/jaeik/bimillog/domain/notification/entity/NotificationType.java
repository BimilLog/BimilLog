package jaeik.bimillog.domain.notification.entity;

/**
 * <h2>알림 유형</h2>
 * <p>알림의 유형을 정의하는 열거형</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
public enum NotificationType {
    PAPER, // 롤링페이퍼에 메시지가 달렸을 때
    COMMENT, // 게시글에 댓글이 달렸을 때
    POST_FEATURED, // 인기글이 되었을 때
    INITIATE, // SSE 초기화 용
    ADMIN
}

