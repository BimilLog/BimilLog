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
    INVALID_REPORT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 신고 유형입니다.", ErrorCode.LogLevel.WARN),
    INVALID_REPORT_CONTENT(HttpStatus.BAD_REQUEST, "신고 내용이 유효하지 않습니다. 10-500자 사이로 입력해주세요.", ErrorCode.LogLevel.WARN),
    INVALID_REPORT_TARGET(HttpStatus.BAD_REQUEST, "신고 대상이 유효하지 않습니다.", ErrorCode.LogLevel.WARN),
    REPORT_TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "게시글/댓글 신고 시에는 대상 ID가 필요합니다.", ErrorCode.LogLevel.WARN),
    REPORT_TARGET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "오류/개선 신고 시에는 대상 ID를 입력할 수 없습니다.", ErrorCode.LogLevel.WARN),
    
    /**
     * <h3>사용자 제재 관련 에러 코드</h3>
     */
    BAN_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "제재할 대상 사용자를 찾을 수 없습니다.", ErrorCode.LogLevel.WARN),
    INVALID_BAN_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 제재 요청입니다.", ErrorCode.LogLevel.WARN),
    
    /**
     * <h3>관리자 권한 관련 에러 코드</h3>
     */
    ADMIN_PERMISSION_REQUIRED(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.", ErrorCode.LogLevel.WARN),
    ADMIN_ACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "관리자 작업 처리 중 오류가 발생했습니다.", ErrorCode.LogLevel.ERROR),
    
    /**
     * <h3>관리자 도메인에서 필요한 외부 도메인 에러 코드 (임시)</h3>
     * <p>추후 리팩토링 예정</p>
     */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시글이 존재하지 않습니다.", ErrorCode.LogLevel.INFO),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", ErrorCode.LogLevel.INFO);

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