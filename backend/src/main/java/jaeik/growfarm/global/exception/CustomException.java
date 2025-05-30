package jaeik.growfarm.global.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final String target;
    private final String message;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
        this.target = extractTarget();
        this.message = errorCode.getMessage();
    }

    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.status = errorCode.getStatus();
        this.message = errorCode.getMessage();
        this.target = extractTarget();
    }

    public CustomException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.target = extractTarget();
        this.message = message;
    }

    private String extractTarget() {
        StackTraceElement[] stackTrace = this.getStackTrace();
        if (stackTrace.length >= 1) {
            return stackTrace[0].getMethodName();
        } else {
            return "UnknownMethod";
        }
    }
}
