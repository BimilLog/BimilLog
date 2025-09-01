package jaeik.bimillog.domain.comment.application.port.in;

/**
 * <h2>댓글 추천 요구사항</h2>
 * <p>댓글 추천 관련 요청을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentLikeUseCase {

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>댓글에 추천을 누르거나 취소합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 사용자
     * @param commentId 추천/취소할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void likeComment(Long userId, Long commentId);
}
