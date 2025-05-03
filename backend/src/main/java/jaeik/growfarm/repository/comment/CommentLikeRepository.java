package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.board.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*
 * 댓글 추천 Repository
 * 댓글 좋아요 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    int countByCommentId(Long commentId);

    void deleteAllByCommentId(Long commentId);
}
