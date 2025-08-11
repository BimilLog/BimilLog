package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.entity.comment.CommentClosure;

public interface SaveCommentClosurePort {

    void save(CommentClosure commentClosure);
}
