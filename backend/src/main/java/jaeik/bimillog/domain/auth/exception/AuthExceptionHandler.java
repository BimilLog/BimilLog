package jaeik.bimillog.domain.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>인증 도메인 예외 처리기</h2>
 * <p>
 * 인증 도메인에서 발생하는 커스텀 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    /**
     * <h3>인증 도메인 커스텀 예외 처리</h3>
     * <p>인증 도메인에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 인증 커스텀 예외
     * @return 인증 커스텀 예외에 대한 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(AuthCustomException.class)
    public ResponseEntity<AuthErrorResponse> handleAuthCustomException(AuthCustomException e) {
        AuthErrorResponse response = new AuthErrorResponse(
                e.getAuthErrorCode().getStatus().value(),
                "AuthError",
                e.getMessage());

        String logMessage = "AuthCustomException: 코드: {}, 메시지: {}";
        switch (e.getAuthErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getAuthErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getAuthErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getAuthErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getAuthErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getAuthErrorCode().getStatus());
    }
}