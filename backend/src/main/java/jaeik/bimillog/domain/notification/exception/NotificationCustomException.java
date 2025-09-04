package jaeik.bimillog.domain.notification.exception;

import lombok.Getter;

/**
 * <h2>알림 도메인 전용 커스텀 예외</h2>
 * <p>알림(Notification) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>NotificationErrorCode와 함께 사용되어 알림 기능 관련 예외를 명확하게 분리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class NotificationCustomException extends RuntimeException {

    private final NotificationErrorCode notificationErrorCode;

    /**
     * <h3>알림 커스텀 예외 생성자</h3>
     * <p>NotificationErrorCode를 받아 알림 도메인 전용 예외를 생성합니다.</p>
     *
     * @param notificationErrorCode 알림 전용 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public NotificationCustomException(NotificationErrorCode notificationErrorCode) {
        super(notificationErrorCode.getMessage());
        this.notificationErrorCode = notificationErrorCode;
    }

    /**
     * <h3>알림 커스텀 예외 생성자 (메시지 포함)</h3>
     * <p>NotificationErrorCode와 추가 메시지를 받아 알림 도메인 전용 예외를 생성합니다.</p>
     *
     * @param notificationErrorCode 알림 전용 에러 코드
     * @param message 추가 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    public NotificationCustomException(NotificationErrorCode notificationErrorCode, String message) {
        super(message);
        this.notificationErrorCode = notificationErrorCode;
    }

    /**
     * <h3>알림 커스텀 예외 생성자 (원인 포함)</h3>
     * <p>NotificationErrorCode와 원인 예외를 받아 알림 도메인 전용 예외를 생성합니다.</p>
     *
     * @param notificationErrorCode 알림 전용 에러 코드
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public NotificationCustomException(NotificationErrorCode notificationErrorCode, Throwable cause) {
        super(notificationErrorCode.getMessage(), cause);
        this.notificationErrorCode = notificationErrorCode;
    }

    /**
     * <h3>알림 커스텀 예외 생성자 (전체)</h3>
     * <p>NotificationErrorCode, 추가 메시지, 원인 예외를 모두 받아 알림 도메인 전용 예외를 생성합니다.</p>
     *
     * @param notificationErrorCode 알림 전용 에러 코드
     * @param message 추가 메시지
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public NotificationCustomException(NotificationErrorCode notificationErrorCode, String message, Throwable cause) {
        super(message, cause);
        this.notificationErrorCode = notificationErrorCode;
    }
}