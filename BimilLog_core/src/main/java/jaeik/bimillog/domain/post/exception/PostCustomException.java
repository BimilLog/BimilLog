package jaeik.bimillog.domain.post.exception;

import lombok.Getter;

/**
 * <h2>게시글 도메인 전용 커스텀 예외</h2>
 * <p>게시글(Post) 도메인에서 발생하는 비즈니스 예외를 처리하는 커스텀 예외 클래스</p>
 * <p>PostQueryController, PostCommandController에서 게시글 관련 비즈니스 로직 오류 시 발생합니다.</p>
 * <p>PostExceptionHandler에서 처리되어 적절한 HTTP 상태 코드와 오류 메시지로 변환됩니다.</p>
 * <p>PostErrorCode와 함께 사용되어 게시글 예외를 다른 도메인 예외와 명확히 구분합니다.</p>
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
     * <p>PostService나 PostRepository에서 게시글 비즈니스 로직 오류 시 호출됩니다.</p>
     * <p>오류 메시지는 PostErrorCode에 정의된 기본 메시지를 사용합니다.</p>
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
     * <p>PostErrorCode와 커스텀 메시지를 받아 게시글 도메인 전용 예외를 생성합니다.</p>
     * <p>기본 에러 메시지가 아닌 구체적이고 상세한 에러 정보가 필요한 경우 사용됩니다.</p>
     * <p>주로 유효성 검증 실패나 동적 에러 메시지가 필요한 상황에서 활용됩니다.</p>
     *
     * @param postErrorCode 게시글 전용 에러 코드
     * @param message 커스텀 에러 메시지
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
     * <p>데이터베이스 연결 오류, 레디스 오류 등 하위 시스템 예외가 원인인 경우 사용됩니다.</p>
     * <p>원인 예외를 체이닝하여 디버깅과 로그 추적을 용이하게 합니다.</p>
     *
     * @param postErrorCode 게시글 전용 에러 코드
     * @param cause 원인 예외 (DB 오류, Redis 오류 등)
     * @author Jaeik
     * @since 2.0.0
     */
    public PostCustomException(PostErrorCode postErrorCode, Throwable cause) {
        super(postErrorCode.getMessage(), cause);
        this.postErrorCode = postErrorCode;
    }
}