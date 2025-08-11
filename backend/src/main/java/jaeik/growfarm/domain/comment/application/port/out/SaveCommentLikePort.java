package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.domain.CommentLike;

public interface SaveCommentLikePort {
    CommentLike save(CommentLike commentLike);
}
