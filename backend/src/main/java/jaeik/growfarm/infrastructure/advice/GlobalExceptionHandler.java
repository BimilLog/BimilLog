package jaeik.growfarm.infrastructure.advice;

import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unknown",
                e.getMessage());
        log.error("Exception: 메시지: {}, 스택트레이스: {}", e.getMessage(), e.getClass().getSimpleName(), e);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
