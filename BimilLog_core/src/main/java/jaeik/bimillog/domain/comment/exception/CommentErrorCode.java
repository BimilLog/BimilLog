package jaeik.bimillog.domain.comment.exception;

import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <h2>댓글 도메인 전용 에러 코드</h2>
 * <p>댓글(Comment) 도메인에서 발생할 수 있는 전용 에러 코드를 정의하는 열거형</p>
 * <p>댓글 작성, 수정, 삭제, 조회, 좋아요 등 댓글과 관련된 에러 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum CommentErrorCode {

    /**
     * <h3>댓글 조회 관련 에러 코드</h3>
     */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 댓글을 찾을 수 없습니다.", ErrorCode.LogLevel.INFO),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글이 존재하지 않습니다.", ErrorCode.LogLevel.INFO),
    COMMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 조회에 실패했습니다.", ErrorCode.LogLevel.ERROR),
    
    /**
     * <h3>댓글 작성 관련 에러 코드</h3>
     */
    COMMENT_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 작성에 실패했습니다.", ErrorCode.LogLevel.ERROR),
    
    /**
     * <h3>댓글 수정/삭제 관련 에러 코드</h3>
     */
    COMMENT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다.", ErrorCode.LogLevel.INFO);

    private final HttpStatus status;
    private final String message;
    private final ErrorCode.LogLevel logLevel;

    /**
     * <h3>CommentErrorCode 생성자</h3>
     * <p>HTTP 상태, 메시지, 로그 레벨을 받아 댓글 전용 에러 코드를 생성합니다.</p>
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param logLevel 에러 로그 레벨
     * @author Jaeik
     * @since 2.0.0
     */
    CommentErrorCode(HttpStatus status, String message, ErrorCode.LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}