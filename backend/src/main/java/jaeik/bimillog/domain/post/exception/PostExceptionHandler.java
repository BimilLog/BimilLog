package jaeik.bimillog.domain.post.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>게시글 도메인 예외 처리기</h2>
 * <p>
 * 게시글 도메인에서 발생하는 PostCustomException을 전역적으로 처리하는 클래스
 * </p>
 * <p>@RestControllerAdvice로 설정되어 모든 Controller에서 발생하는 게시글 예외를 쪽아서 처리합니다.</p>
 * <p>PostCustomException을 PostErrorResponse로 변환하여 일관된 HTTP 에러 응답을 제공합니다.</p>
 * <p>PostErrorCode의 로그 레벨에 따라 적절한 로깅을 수행하여 시스템 모니터링을 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class PostExceptionHandler {

    /**
     * <h3>게시글 도메인 커스텀 예외 처리</h3>
     * <p>게시글 Controller나 Service에서 발생하는 PostCustomException을 전역적으로 처리합니다.</p>
     * <p>예외에 포함된 PostErrorCode를 기반으로 HTTP 상태와 에러 메시지를 설정합니다.</p>
     * <p>로그 레벨(INFO/WARN/ERROR/FATAL)에 따라 적절한 로깅을 수행하여 시스템 모니터링을 지원합니다.</p>
     * <p>PostErrorResponse 형식으로 일관된 에러 응답을 클라이언트에게 제공합니다.</p>
     *
     * @param e 발생한 PostCustomException
     * @return PostErrorResponse로 래핑된 HTTP 응답 엔티티
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
        }

        return new ResponseEntity<>(response, e.getPostErrorCode().getStatus());
    }
}