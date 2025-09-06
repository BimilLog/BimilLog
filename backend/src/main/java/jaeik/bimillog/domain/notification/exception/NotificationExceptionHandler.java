package jaeik.bimillog.domain.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>알림 도메인 예외 처리기</h2>
 * <p>
 * 알림 도메인에서 발생하는 커스텀 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class NotificationExceptionHandler {

    /**
     * <h3>알림 도메인 커스텀 예외 처리</h3>
     * <p>알림 도메인에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 알림 커스텀 예외
     * @return 알림 커스텀 예외에 대한 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(NotificationCustomException.class)
    public ResponseEntity<NotificationErrorResponse> handleNotificationCustomException(NotificationCustomException e) {
        NotificationErrorResponse response = new NotificationErrorResponse(
                e.getNotificationErrorCode().getStatus().value(),
                "NotificationError",
                e.getMessage());

        String logMessage = "NotificationCustomException: 코드: {}, 메시지: {}";
        switch (e.getNotificationErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getNotificationErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getNotificationErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getNotificationErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getNotificationErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getNotificationErrorCode().getStatus());
    }
}