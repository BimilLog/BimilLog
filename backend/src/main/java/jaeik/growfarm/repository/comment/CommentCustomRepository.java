package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.board.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/*
 * 커스텀 댓글 Repository
 * 댓글 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
public interface CommentCustomRepository {

    List<Comment> findByCommentList(Long postId);

    Page<Comment> findByLikedComments(Long userId, Pageable pageable);

}
