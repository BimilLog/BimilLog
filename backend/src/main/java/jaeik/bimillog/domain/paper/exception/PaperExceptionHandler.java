package jaeik.bimillog.domain.paper.exception;

import jaeik.bimillog.infrastructure.advice.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>롤링페이퍼 도메인 예외 처리기</h2>
 * <p>
 * 롤링페이퍼 도메인에서 발생하는 커스텀 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class PaperExceptionHandler {

    /**
     * <h3>롤링페이퍼 도메인 커스텀 예외 처리</h3>
     * <p>롤링페이퍼 도메인에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 롤링페이퍼 커스텀 예외
     * @return 롤링페이퍼 커스텀 예외에 대한 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(PaperCustomException.class)
    public ResponseEntity<ErrorResponse> handlePaperCustomException(PaperCustomException e) {
        ErrorResponse response = new ErrorResponse(
                e.getPaperErrorCode().getStatus().value(),
                "PaperError",
                e.getMessage());

        String logMessage = "PaperCustomException: 코드: {}, 메시지: {}";
        switch (e.getPaperErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getPaperErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getPaperErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getPaperErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getPaperErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getPaperErrorCode().getStatus());
    }
}