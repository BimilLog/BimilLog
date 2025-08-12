package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentClosure;

public interface SaveCommentClosurePort {
    void save(CommentClosure commentClosure);
}
