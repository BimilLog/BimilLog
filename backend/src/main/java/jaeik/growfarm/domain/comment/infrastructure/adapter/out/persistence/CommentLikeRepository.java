package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.comment.domain.CommentLike;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.domain.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentAndUser(Comment comment, User user);

    void deleteByCommentAndUser(Comment comment, User user);
}





