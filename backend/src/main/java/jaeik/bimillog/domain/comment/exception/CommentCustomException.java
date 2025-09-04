package jaeik.bimillog.domain.comment.exception;

import lombok.Getter;

/**
 * <h2>댓글 도메인 전용 커스텀 예외</h2>
 * <p>댓글(Comment) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>CommentErrorCode와 함께 사용되어 댓글 기능 관련 예외를 명확하게 분리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class CommentCustomException extends RuntimeException {

    private final CommentErrorCode commentErrorCode;

    /**
     * <h3>댓글 커스텀 예외 생성자</h3>
     * <p>CommentErrorCode를 받아 댓글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param commentErrorCode 댓글 전용 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentCustomException(CommentErrorCode commentErrorCode) {
        super(commentErrorCode.getMessage());
        this.commentErrorCode = commentErrorCode;
    }

    /**
     * <h3>댓글 커스텀 예외 생성자 (메시지 포함)</h3>
     * <p>CommentErrorCode와 추가 메시지를 받아 댓글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param commentErrorCode 댓글 전용 에러 코드
     * @param message 추가 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentCustomException(CommentErrorCode commentErrorCode, String message) {
        super(message);
        this.commentErrorCode = commentErrorCode;
    }

    /**
     * <h3>댓글 커스텀 예외 생성자 (원인 포함)</h3>
     * <p>CommentErrorCode와 원인 예외를 받아 댓글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param commentErrorCode 댓글 전용 에러 코드
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentCustomException(CommentErrorCode commentErrorCode, Throwable cause) {
        super(commentErrorCode.getMessage(), cause);
        this.commentErrorCode = commentErrorCode;
    }

    /**
     * <h3>댓글 커스텀 예외 생성자 (전체)</h3>
     * <p>CommentErrorCode, 추가 메시지, 원인 예외를 모두 받아 댓글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param commentErrorCode 댓글 전용 에러 코드
     * @param message 추가 메시지
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentCustomException(CommentErrorCode commentErrorCode, String message, Throwable cause) {
        super(message, cause);
        this.commentErrorCode = commentErrorCode;
    }
}