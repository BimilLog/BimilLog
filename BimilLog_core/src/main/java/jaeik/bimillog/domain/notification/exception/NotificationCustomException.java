package jaeik.bimillog.domain.notification.exception;

import lombok.Getter;

/**
 * <h2>알림 도메인 전용 커스텀 예외</h2>
 * <p>알림(Notification) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>FCM 전송 실패, SSE 연결 오류, 알림 대상 사용자 조회 실패 등 알림 관련 예외 발생 시 NotificationService에서 발생시켜 예외를 명확하게 분리 처리하는 클래스</p>
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
     * <p>FCM 전송 실패, 알림 대상 사용자 조회 실패 등의 상황에서 NotificationService에서 호출되어 알림 관련 비즈니스 예외를 생성하는 생성자</p>
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
     * <h3>알림 커스텀 예외 생성자 (원인 포함)</h3>
     * <p>NotificationErrorCode와 원인 예외를 받아 알림 도메인 전용 예외를 생성합니다.</p>
     * <p>FCM API 호출 실패, 데이터베이스 연결 오류 등의 외부 시스템 오류로 인한 예외가 발생했을 때 NotificationService에서 원인 예외를 포함하여 호출되는 생성자</p>
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
}