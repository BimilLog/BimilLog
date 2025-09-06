package jaeik.bimillog.infrastructure.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * <h2>커스텀 예외 클래스</h2>
 * <p>
 * 애플리케이션에서 발생하는 다양한 예외를 처리하기 위한 커스텀 예외 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final String target;
    private final String message;
    private final ErrorCode errorCode;

    /**
     * <h3>CustomException 생성자</h3>
     * <p>ErrorCode를 받아 예외를 생성합니다.</p>
     *
     * @param errorCode 발생한 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
        this.target = extractTarget();
        this.message = errorCode.getMessage();
    }

    /**
     * <h3>메소드 이름 추출</h3>
     * <p>
     * 현재 예외가 발생한 메소드의 이름을 추출하여 반환
     * </p>
     *
     * @return 현재 예외가 발생한 메소드의 이름
     * @since 2.0.0
     * @author Jaeik
     */
    private String extractTarget() {
        StackTraceElement[] stackTrace = this.getStackTrace();
        if (stackTrace.length >= 1) {
            return stackTrace[0].getMethodName();
        } else {
            return "UnknownMethod";
        }
    }
}
