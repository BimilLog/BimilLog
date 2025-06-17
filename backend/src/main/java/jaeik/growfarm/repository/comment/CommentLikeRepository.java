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
 * @version  1.0.0
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /**
     * <h3>댓글 추천 여부 조회</h3>
     * <p>특정 댓글에 대해 사용자가 추천했는지 여부를 조회한다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return 추천 정보가 존재하면 Optional에 포함된 CommentLike 객체, 없으면 Optional.empty()
     * @since 1.0.0
     * @author Jaeik
     */
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * <h3>댓글 추천 수 조회</h3>
     * <p>특정 댓글에 대한 추천 수를 조회한다.</p>
     *
     * @param commentId 댓글 ID
     * @return 추천 수
     * @since 1.0.0
     * @author Jaeik
     */
    int countByCommentId(Long commentId);
}
