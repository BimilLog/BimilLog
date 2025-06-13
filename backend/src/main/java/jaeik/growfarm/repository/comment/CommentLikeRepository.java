package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>댓글 추천 레포지토리</h2>
 * <p>댓글에 대한 추천 정보를 관리하는 레포지토리</p>
 * <p>댓글과 사용자 간의 추천 관계를 저장하고 조회하는 기능을 제공한다.</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    int countByCommentId(Long commentId);
}
