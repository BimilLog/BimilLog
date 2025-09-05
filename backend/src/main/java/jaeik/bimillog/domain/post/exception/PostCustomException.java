package jaeik.bimillog.domain.post.exception;

import lombok.Getter;

/**
 * <h2>게시글 도메인 전용 커스텀 예외</h2>
 * <p>게시글(Post) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>PostErrorCode와 함께 사용되어 게시글 기능 관련 예외를 명확하게 분리 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class PostCustomException extends RuntimeException {

    private final PostErrorCode postErrorCode;

    /**
     * <h3>게시글 커스텀 예외 생성자</h3>
     * <p>PostErrorCode를 받아 게시글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param postErrorCode 게시글 전용 에러 코드
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCustomException(PostErrorCode postErrorCode) {
        super(postErrorCode.getMessage());
        this.postErrorCode = postErrorCode;
    }

    /**
     * <h3>게시글 커스텀 예외 생성자 (메시지 포함)</h3>
     * <p>PostErrorCode와 추가 메시지를 받아 게시글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param postErrorCode 게시글 전용 에러 코드
     * @param message 추가 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCustomException(PostErrorCode postErrorCode, String message) {
        super(message);
        this.postErrorCode = postErrorCode;
    }

    /**
     * <h3>게시글 커스텀 예외 생성자 (원인 포함)</h3>
     * <p>PostErrorCode와 원인 예외를 받아 게시글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param postErrorCode 게시글 전용 에러 코드
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCustomException(PostErrorCode postErrorCode, Throwable cause) {
        super(postErrorCode.getMessage(), cause);
        this.postErrorCode = postErrorCode;
    }

    /**
     * <h3>게시글 커스텀 예외 생성자 (전체)</h3>
     * <p>PostErrorCode, 추가 메시지, 원인 예외를 모두 받아 게시글 도메인 전용 예외를 생성합니다.</p>
     *
     * @param postErrorCode 게시글 전용 에러 코드
     * @param message 추가 메시지
     * @param cause 원인 예외
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCustomException(PostErrorCode postErrorCode, String message, Throwable cause) {
        super(message, cause);
        this.postErrorCode = postErrorCode;
    }
}