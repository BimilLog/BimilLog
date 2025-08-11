package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.entity.comment.CommentLike;

public interface DeleteCommentLikePort {

    void delete(CommentLike commentLike);
}
