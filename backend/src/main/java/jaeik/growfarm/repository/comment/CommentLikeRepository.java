package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.board.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    int countByCommentId(Long commentId);

    void deleteAllByCommentId(Long commentId);
}


