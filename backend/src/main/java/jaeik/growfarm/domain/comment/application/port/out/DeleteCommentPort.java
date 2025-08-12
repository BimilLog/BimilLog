package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.user.entity.User;

public interface DeleteCommentPort {

    void delete(Comment comment);

    void deleteById(Long commentId);

    void deleteLike(Comment comment, User user);

    void deleteAllByPostId(Long postId);
}
