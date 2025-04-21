package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.board.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentCustomRepository {

    List<Comment> findByCommentList(Long postId);

    Page<Comment> findByLikedComments(Long userId, Pageable pageable);


}
