package jaeik.bimillog.domain.admin.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>관리자 도메인 전용 에러 코드</h2>
 * <p>관리자(Admin) 도메인에서 발생할 수 있는 전용 에러 코드를 정의하는 열거형</p>
 * <p>신고, 제재, 사용자 관리 등 관리자 기능과 관련된 에러 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum AdminErrorCode {

    /**
     * <h3>신고 관련 에러 코드</h3>
     */
    INVALID_REPORT_TARGET(HttpStatus.BAD_REQUEST, "신고 대상이 유효하지 않습니다.", ErrorCode.LogLevel.WARN),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저가 존재하지 않습니다.", ErrorCode.LogLevel.WARN);

    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>AdminErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 관리자 전용 에러 코드를 생성합니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    AdminErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}