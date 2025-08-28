package jaeik.growfarm.infrastructure.advice;

import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * <h2>글로벌 예외 처리기</h2>
 * <p>
 * 애플리케이션 전역에서 발생하는 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * <h3>커스텀 예외 처리</h3>
     * <p>
     * 애플리케이션에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환
     * </p>
     *
     * @param e 발생한 커스텀 예외
     * @return 커스텀 예외에 대한 응답 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorResponse response = new ErrorResponse(
                e.getStatus().value(),
                e.getTarget(),
                e.getMessage());

        // ErrorCode의 LogLevel에 따라 적절한 로그 레벨로 기록
        if (e.getErrorCode() != null) {
            String logMessage = "CustomException: 코드: {}, 타겟: {}, 메시지: {}";
            ErrorCode.LogLevel logLevel = e.getErrorCode().getLogLevel();

            switch (logLevel) {
                case INFO -> log.info(logMessage, e.getErrorCode().name(), e.getTarget(), e.getMessage());
                case WARN -> log.warn(logMessage, e.getErrorCode().name(), e.getTarget(), e.getMessage());
                case ERROR -> log.error(logMessage, e.getErrorCode().name(), e.getTarget(), e.getMessage());
                case FATAL ->
                    log.error("FATAL - " + logMessage, e.getErrorCode().name(), e.getTarget(), e.getMessage());
            }
        } else {
            // ErrorCode가 없는 경우 기본적으로 ERROR 레벨로 로깅
            log.error("CustomException: 타겟: {}, 메시지: {}", e.getTarget(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getStatus());
    }

    /**
     * <h3>일반 예외 처리</h3>
     * <p>
     * 애플리케이션에서 발생하는 일반 예외를 처리하여 적절한 응답을 반환
     * </p>
     *
     * @param e 발생한 일반 예외
     * @return 일반 예외에 대한 응답 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (e instanceof ResponseStatusException ex) {
            status = (HttpStatus) ex.getStatusCode();
        } else if (e.getClass().isAnnotationPresent(ResponseStatus.class)) {
            status = e.getClass().getAnnotation(ResponseStatus.class).value();
        }

        ErrorResponse response = new ErrorResponse(
                status.value(),
                e.getClass().getSimpleName(),
                e.getMessage());

        log.error("Exception: {}", e.getMessage(), e);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * <h3>시큐리티 권한 부족 예외 처리</h3>
     * 시큐리티의 권한 에러를 잡는다.
     * @param
     * @return
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                e.getClass().getSimpleName(),
                e.getMessage()
        );
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * <h3>Spring Security 인증 실패 예외 처리</h3>
     * 인증되지 않은 사용자가 접근했을 때 401 Unauthorized 반환
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                e.getClass().getSimpleName(),
                e.getMessage()
        );
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * <h3>Bean Validation 예외 처리</h3>
     * <p>@Valid 검증 실패 시 400 Bad Request 반환</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 첫 번째 validation 에러 메시지 사용
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Validation failed");

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getClass().getSimpleName(),
                errorMessage
        );
        
        log.warn("ValidationException: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}
