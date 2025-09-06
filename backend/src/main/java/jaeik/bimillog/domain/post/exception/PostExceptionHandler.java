package jaeik.bimillog.domain.post.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>게시글 도메인 예외 처리기</h2>
 * <p>
 * 게시글 도메인에서 발생하는 커스텀 예외를 처리하는 클래스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class PostExceptionHandler {

    /**
     * <h3>게시글 도메인 커스텀 예외 처리</h3>
     * <p>게시글 도메인에서 발생하는 커스텀 예외를 처리하여 적절한 응답을 반환</p>
     *
     * @param e 발생한 게시글 커스텀 예외
     * @return 게시글 커스텀 예외에 대한 응답 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @ExceptionHandler(PostCustomException.class)
    public ResponseEntity<PostErrorResponse> handlePostCustomException(PostCustomException e) {
        PostErrorResponse response = new PostErrorResponse(
                e.getPostErrorCode().getStatus().value(),
                "PostError",
                e.getMessage());

        String logMessage = "PostCustomException: 코드: {}, 메시지: {}";
        switch (e.getPostErrorCode().getLogLevel()) {
            case INFO -> log.info(logMessage, e.getPostErrorCode().name(), e.getMessage());
            case WARN -> log.warn(logMessage, e.getPostErrorCode().name(), e.getMessage());
            case ERROR -> log.error(logMessage, e.getPostErrorCode().name(), e.getMessage());
            case FATAL -> log.error("FATAL - " + logMessage, e.getPostErrorCode().name(), e.getMessage());
        }

        return new ResponseEntity<>(response, e.getPostErrorCode().getStatus());
    }
}