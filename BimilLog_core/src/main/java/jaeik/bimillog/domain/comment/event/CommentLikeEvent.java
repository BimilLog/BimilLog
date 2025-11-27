package jaeik.bimillog.domain.comment.event;

/**
 * <h2>댓글 추천 이벤트</h2>
 * <p>댓글이 추천(좋아요)되었을 때 발생하는 비동기 이벤트</p>
 * <p>CommentCommandService에서 댓글 추천 시 발생합니다.</p>
 * <p>상호작용 점수 증가에 사용됩니다.</p>
 * <p>추천 취소 시에는 이벤트를 발행하지 않습니다 (점수 유지).</p>
 *
 * @param commentId 추천된 댓글 ID
 * @param commentAuthorId 댓글 작성자 ID (익명인 경우 null)
 * @param likerId 추천한 사용자 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record CommentLikeEvent(
        Long commentId,
        Long commentAuthorId,
        Long likerId
) {
    public CommentLikeEvent {
        if (commentId == null) {
            throw new IllegalArgumentException("댓글 ID는 null일 수 없습니다.");
        }
        if (likerId == null) {
            throw new IllegalArgumentException("추천한 사용자 ID는 null일 수 없습니다.");
        }
        // commentAuthorId는 익명 댓글의 경우 null일 수 있음
    }
}
