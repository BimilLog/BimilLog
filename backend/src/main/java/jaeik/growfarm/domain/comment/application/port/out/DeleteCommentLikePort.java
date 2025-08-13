package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentLike;

/**
 * <h2>댓글 좋아요 삭제 포트</h2>
 * <p>댓글 좋아요 엔티티 삭제를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface DeleteCommentLikePort {
    /**
     * <h3>댓글 좋아요 삭제</h3>
     * <p>주어진 댓글 좋아요 엔티티를 삭제합니다.</p>
     *
     * @param commentLike 삭제할 댓글 좋아요 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void delete(CommentLike commentLike);
}
