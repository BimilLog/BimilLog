package jaeik.bimillog.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>에러 코드 열거형</h2>
 * <p>
 * 애플리케이션에서 발생할 수 있는 다양한 에러 코드를 정의하는 열거형
 * </p>
 *
 * @author Jaeik
 * @version  1.0.20
 */
@Getter
public enum ErrorCode {

    /**
     * <h3>인증 관련 에러 코드</h3>
     */
    REPEAT_LOGIN(HttpStatus.FORBIDDEN, "다른기기에서 로그아웃 하셨습니다 다시 로그인 해주세요", LogLevel.INFO),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰을 찾을 수 없습니다", LogLevel.WARN),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "토큰 불일치 - 보안 위협 감지", LogLevel.ERROR),
    SUSPICIOUS_ACTIVITY(HttpStatus.UNAUTHORIZED, "의심스러운 활동 감지 - 모든 세션이 무효화되었습니다", LogLevel.ERROR);

    private final HttpStatus status;
    private final String message;
    private final LogLevel logLevel;


    /**
     * <h3>ErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 에러 코드를 생성합니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    ErrorCode(HttpStatus status, String message, LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }

    /**
     * <h3>로그 레벨 열거형</h3>
     * <p>
     * 에러의 심각도를 나타내는 로그 레벨
     * </p>
     */
    @Getter
    public enum LogLevel {
        /**
         * 정보성 메시지 - 정상적인 동작이지만 기록할 필요가 있는 경우
         */
        INFO,

        /**
         * 경고 메시지 - 잠재적인 문제나 주의가 필요한 상황
         */
        WARN,

        /**
         * 에러 메시지 - 오류 발생으로 기능 실행에 문제가 있는 경우
         */
        ERROR,

        /**
         * 치명적 에러 - 시스템 전체에 영향을 주는 심각한 오류
         */
        FATAL
    }
}
