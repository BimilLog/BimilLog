package jaeik.bimillog.infrastructure.adapter.out.comment.jpa;

import jaeik.bimillog.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * <h2>댓글 레포지토리 인터페이스</h2>
 * <p>
 * 댓글(`Comment`) 엔티티의 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {


    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);


    /**
     * <h3>클로저 테이블에서 자손 관계 삭제</h3>
     * <p>자손이 없는 댓글의 모든 클로저 관계를 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment_closure WHERE descendant_id = :commentId", nativeQuery = true)
    void deleteClosuresByDescendantId(@Param("commentId") Long commentId);

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글을 완전히 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment WHERE comment_id = :commentId", nativeQuery = true)
    void hardDeleteComment(@Param("commentId") Long commentId);


}

