package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.entity.comment.CommentClosure;

import java.util.List;
import java.util.Optional;

public interface LoadCommentClosurePort {

    Optional<List<CommentClosure>> findByDescendantId(Long descendantId);

    boolean hasDescendants(Long commentId);
}
