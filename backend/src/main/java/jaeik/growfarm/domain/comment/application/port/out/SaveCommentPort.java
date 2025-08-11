package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.domain.Comment;

public interface SaveCommentPort {

    Comment save(Comment comment);

    void anonymizeUserComments(Long userId);
}
