package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.domain.global.event.CacheCountEvent;
import jaeik.bimillog.domain.global.event.RealtimeScoreEvent;

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
public record CommentDeletedEvent(Long postId) implements RealtimeScoreEvent, CacheCountEvent {
    @Override
    public double realtimeScore() { return -3.0; }

    @Override
    public String counterField() { return "commentCount"; }

    @Override
    public int counterDelta() { return -1; }
}
