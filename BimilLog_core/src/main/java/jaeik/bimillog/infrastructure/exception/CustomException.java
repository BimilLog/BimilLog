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
public class CustomException extends RuntimeException {
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
    }
}
