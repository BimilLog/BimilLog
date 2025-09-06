package jaeik.bimillog.domain.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>사용자 도메인 예외 처리기</h2>
 * <p>
 * 사용자 도메인에서 발생하는 커스텀 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class UserExceptionHandler {

    /**
     * <h3>사용자 도메인 커스텀 예외 처리</h3>
     * <p>사용자 도메인에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 사용자 커스텀 예외
     * @return 사용자 커스텀 예외에 대한 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(UserCustomException.class)
    public ResponseEntity<UserErrorResponse> handleUserCustomException(UserCustomException e) {
        UserErrorResponse response = new UserErrorResponse(
                e.getUserErrorCode().getStatus().value(),
                "UserError",
                e.getMessage());

        String logMessage = "UserCustomException: 코드: {}, 메시지: {}";
        switch (e.getUserErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getUserErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getUserErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getUserErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getUserErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getUserErrorCode().getStatus());
    }
}