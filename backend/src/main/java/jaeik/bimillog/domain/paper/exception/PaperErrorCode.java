package jaeik.bimillog.domain.paper.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>롤링페이퍼 도메인 에러 코드</h2>
 * <p>
 * 롤링페이퍼 도메인에서 발생할 수 있는 에러 코드를 정의하는 열거형
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum PaperErrorCode {

    /**
     * <h3>롤링페이퍼 조회 에러</h3>
     */
    USERNAME_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 닉네임의 롤링페이퍼를 찾을 수 없습니다.", ErrorCode.LogLevel.INFO),

    /**
     * <h3>메시지 조회/삭제 에러</h3>
     */
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다.", ErrorCode.LogLevel.INFO),
    MESSAGE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 롤링페이퍼의 메시지만 삭제할 수 있습니다.", ErrorCode.LogLevel.INFO),

    /**
     * <h3>입력값 검증 에러</h3>
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다.", ErrorCode.LogLevel.WARN);


    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>PaperErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 롤링페이퍼 에러 코드를 생성합니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    PaperErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}