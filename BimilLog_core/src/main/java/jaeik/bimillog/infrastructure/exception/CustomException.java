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
     * <h3>CustomException 기본 생성자</h3>
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
     * <h3>CustomException 커스텀 메시지 생성자</h3>
     * <p>ErrorCode와 함께 커스텀 메시지를 받아 예외를 생성합니다.</p>
     * <p>기본 에러 메시지 대신 커스텀 메시지를 사용합니다.</p>
     *
     * @param errorCode 발생한 에러 코드
     * @param customMessage 사용자 정의 에러 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
        this.target = extractTarget();
        this.message = customMessage;
    }

    /**
     * <h3>CustomException 원인 예외 포함 생성자</h3>
     * <p>ErrorCode와 함께 원인이 된 예외를 받아 예외를 생성합니다.</p>
     * <p>예외 체이닝을 통해 근본 원인을 추적할 수 있습니다.</p>
     *
     * @param errorCode 발생한 에러 코드
     * @param cause 원인이 된 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
        this.target = extractTarget();
        this.message = errorCode.getMessage();
    }

    /**
     * <h3>CustomException 완전 생성자</h3>
     * <p>ErrorCode, 커스텀 메시지, 원인 예외를 모두 받아 예외를 생성합니다.</p>
     * <p>가장 상세한 예외 정보를 제공합니다.</p>
     *
     * @param errorCode 발생한 에러 코드
     * @param customMessage 사용자 정의 에러 메시지
     * @param cause 원인이 된 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public CustomException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
        this.target = extractTarget();
        this.message = customMessage;
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
