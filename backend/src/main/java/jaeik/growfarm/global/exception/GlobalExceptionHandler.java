package jaeik.growfarm.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>글로벌 예외 처리기</h2>
 * <p>애플리케이션 전역에서 발생하는 예외를 처리하는 클래스</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * <h3>커스텀 예외 처리</h3>
     * <p>애플리케이션에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 커스텀 예외
     * @return 커스텀 예외에 대한 응답 엔티티
     * @since 1.0.0
     * @author Jaeik
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorResponse response = new ErrorResponse(
                e.getStatus().value(),
                e.getTarget(),
                e.getMessage()
        );
        log.error("CustomException: 코드 : {}, 타겟 : {}, 메시지 : {}", e.getMessage(), e.getTarget(), e.getMessage());
        return new ResponseEntity<>(response, e.getStatus());
    }

    /**
     * <h3>일반 예외 처리</h3>
     * <p>애플리케이션에서 발생하는 일반 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 일반 예외
     * @return 일반 예외에 대한 응답 엔티티
     * @since 1.0.0
     * @author Jaeik
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unknown",
                e.getMessage()
        );
        log.error("CustomException: 코드 : {}, 타겟 : {}, 메시지 : {}", e.getMessage(), e.getStackTrace(), e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
