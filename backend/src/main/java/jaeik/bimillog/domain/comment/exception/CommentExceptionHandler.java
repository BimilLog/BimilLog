package jaeik.bimillog.domain.comment.exception;

import jaeik.bimillog.infrastructure.advice.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>댓글 도메인 예외 처리기</h2>
 * <p>
 * 댓글 도메인에서 발생하는 커스텀 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class CommentExceptionHandler {

    /**
     * <h3>댓글 도메인 커스텀 예외 처리</h3>
     * <p>댓글 도메인에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 댓글 커스텀 예외
     * @return 댓글 커스텀 예외에 대한 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(CommentCustomException.class)
    public ResponseEntity<ErrorResponse> handleCommentCustomException(CommentCustomException e) {
        ErrorResponse response = new ErrorResponse(
                e.getCommentErrorCode().getStatus().value(),
                "CommentError",
                e.getMessage());

        String logMessage = "CommentCustomException: 코드: {}, 메시지: {}";
        switch (e.getCommentErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getCommentErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getCommentErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getCommentErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getCommentErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getCommentErrorCode().getStatus());
    }
}