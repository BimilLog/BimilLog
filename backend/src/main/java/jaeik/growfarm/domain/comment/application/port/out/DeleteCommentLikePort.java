package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentLike;

public interface DeleteCommentLikePort {
    void delete(CommentLike commentLike);
}
