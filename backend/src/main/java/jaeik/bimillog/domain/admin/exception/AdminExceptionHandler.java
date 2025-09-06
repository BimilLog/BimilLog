package jaeik.bimillog.domain.admin.exception;

import jaeik.bimillog.infrastructure.advice.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>관리자 도메인 예외 처리기</h2>
 * <p>
 * 관리자 도메인에서 발생하는 커스텀 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class AdminExceptionHandler {

    /**
     * <h3>관리자 도메인 커스텀 예외 처리</h3>
     * <p>관리자 도메인에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 관리자 커스텀 예외
     * @return 관리자 커스텀 예외에 대한 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(AdminCustomException.class)
    public ResponseEntity<ErrorResponse> handleAdminCustomException(AdminCustomException e) {
        ErrorResponse response = new ErrorResponse(
                e.getAdminErrorCode().getStatus().value(),
                "AdminError",
                e.getMessage());

        String logMessage = "AdminCustomException: 코드: {}, 메시지: {}";
        switch (e.getAdminErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getAdminErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getAdminErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getAdminErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getAdminErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getAdminErrorCode().getStatus());
    }
}