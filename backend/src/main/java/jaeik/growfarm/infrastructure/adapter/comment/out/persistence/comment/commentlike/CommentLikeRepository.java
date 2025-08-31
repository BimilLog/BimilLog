package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentlike;

import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 * <h2>댓글 추천 레포지토리 인터페이스</h2>
 * <p>
 * 댓글 추천(`CommentLike`) 엔티티의 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /**
     * <h3>댓글과 사용자로 추천 삭제</h3>
     * <p>주어진 댓글과 사용자에 해당하는 추천를 삭제합니다.</p>
     *
     * @param comment 추천를 삭제할 댓글 엔티티
     * @param user    추천를 삭제할 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByCommentAndUser(Comment comment, User user);

    /**
     * <h3>댓글 ID와 사용자 ID로 추천 존재 여부 확인</h3>
     * <p>주어진 댓글 ID와 사용자 ID에 해당하는 추천이 존재하는지 EXISTS 쿼리로 확인합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return boolean 추천이 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.user.id = :userId")
    boolean existsByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * <h3>댓글 ID와 사용자 ID로 추천 삭제</h3>
     * <p>주어진 댓글 ID와 사용자 ID에 해당하는 추천을 직접 삭제합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.user.id = :userId")
    void deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

}





