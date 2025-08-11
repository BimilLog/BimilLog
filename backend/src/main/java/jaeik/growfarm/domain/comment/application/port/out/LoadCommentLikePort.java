package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.entity.comment.CommentLike;

import java.util.Optional;

public interface LoadCommentLikePort {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
}
