package jaeik.bimillog.domain.comment.event;

/**
 * <h2>댓글 삭제 이벤트</h2>
 * <p>댓글이 삭제되었을 때 발생하는 비동기 이벤트</p>
 * <p>CommentCommandService에서 댓글 삭제 시 발생합니다.</p>
 * <p>실시간 인기글 점수 감소에 사용됩니다.</p>
 *
 * @param postId 댓글이 삭제된 게시글 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record CommentDeletedEvent(Long postId) {
    public CommentDeletedEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
    }
}
