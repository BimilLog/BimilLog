package jaeik.bimillog.domain.notification.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>알림 도메인 전용 에러 코드</h2>
 * <p>알림(Notification) 도메인에서 발생할 수 있는 전용 에러 코드를 정의하는 열거형</p>
 * <p>FCM 푸시 알림 전송 실패, SSE 실시간 알림 오류, 알림 대상 사용자 조회 실패 등 알림 관련 예외 상황에서 NotificationCustomException과 함께 사용되는 열거형</p>
 * <p>FCM 푸시 알림, SSE 실시간 알림, 알림 상태 관리 등 알림과 관련된 에러 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum NotificationErrorCode {

    /**
     * <h3>알림 전송 관련 에러 코드</h3>
     */
    NOTIFICATION_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송 중 오류가 발생했습니다.", ErrorCode.LogLevel.ERROR),
    
    /**
     * <h3>알림 입력값 관련 에러 코드</h3>
     */
    INVALID_USER_CONTEXT(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 컨텍스트입니다.", ErrorCode.LogLevel.WARN),
    NO_SEND_FCM_TOKEN(HttpStatus.BAD_REQUEST, "fcm토큰이 없습니다.", ErrorCode.LogLevel.WARN),
    NO_MEMBER_FCM_TOKEN(HttpStatus.INTERNAL_SERVER_ERROR, "사용자가 존재하지 않습니다.", ErrorCode.LogLevel.WARN);

    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>NotificationErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 알림 전용 에러 코드를 생성합니다.</p>
     * <p>각 알림 관련 예외 상황별로 적절한 HTTP 상태 코드, 사용자에게 표시할 메시지, 서버 로깅 레벨을 정의하여 에러 코드를 초기화하는 생성자</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    NotificationErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}