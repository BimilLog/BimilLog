package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentLike;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadCommentLikePort {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    Map<Long, Long> countByCommentIds(List<Long> commentIds);
}
