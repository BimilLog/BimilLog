package jaeik.bimillog.domain.post.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>게시글 도메인 전용 에러 코드</h2>
 * <p>게시글(Post) 도메인에서 발생할 수 있는 비즈니스 예외 상황을 정의하는 열거형</p>
 * <p>PostCustomException과 함께 사용되어 게시글 관련 오류를 명확하게 분류하고 처리합니다.</p>
 * <p>PostExceptionHandler에서 각 에러 코드에 맞는 HTTP 상태와 메시지, 로그 레벨로 변환됩니다.</p>
 * <p>게시글 작성, 수정, 삭제, 조회, 추천, 검색, 캐시 등 게시글 전 범위의 오류를 포괄합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum PostErrorCode {

    /**
     * <h3>게시글 조회 관련 에러 코드</h3>
     */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 게시글이 존재하지 않습니다.", ErrorCode.LogLevel.INFO),
    
    /**
     * <h3>게시글 입력값 검증 관련 에러 코드</h3>
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다.", ErrorCode.LogLevel.WARN),
    INVALID_SEARCH_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 검색 타입입니다. 허용된 타입: title, content, writer, title_content", ErrorCode.LogLevel.WARN),
    
    /**
     * <h3>게시글 권한 관련 에러 코드</h3>
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다.", ErrorCode.LogLevel.WARN),
    
    /**
     * <h3>게시글 캐시 관련 에러 코드</h3>
     */
    REDIS_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 작성 중 오류가 발생했습니다.", ErrorCode.LogLevel.ERROR),
    REDIS_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 읽기 중 오류가 발생했습니다.", ErrorCode.LogLevel.ERROR),
    REDIS_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "레디스 삭제 중 오류가 발생했습니다.", ErrorCode.LogLevel.ERROR);

    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>PostErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 게시글 전용 에러 코드를 생성합니다.</p>
     * <p>각 에러 상황에 적합한 HTTP 상태와 사용자 친화적 메시지를 설정합니다.</p>
     * <p>로그 레벨에 따라 PostExceptionHandler에서 적절한 로깅 처리가 이뤄집니다.</p>
     *
     * @param status HTTP 상태 코드 (400, 403, 404, 500 등)
     * @param message 사용자에게 노출될 에러 메시지
     * @param logLevel 에러 심각도 (INFO, WARN, ERROR, FATAL)
     * @author Jaeik
     * @since 2.0.0
     */
    PostErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}