package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.domain.Comment;

public interface DeleteCommentPort {

    void delete(Comment comment);

    void deleteById(Long commentId);
}
