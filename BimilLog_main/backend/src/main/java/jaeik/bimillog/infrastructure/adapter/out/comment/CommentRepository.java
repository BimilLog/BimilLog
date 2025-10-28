package jaeik.bimillog.infrastructure.adapter.out.comment;

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
     * <h3>클로저 테이블에서 댓글 관련 모든 관계 삭제</h3>
     * <p>댓글과 관련된 모든 클로저 관계를 삭제합니다.</p>
     * <p>descendant_id와 ancestor_id 양방향으로 참조하는 레코드를 모두 삭제하여 FK 제약 조건 위반을 방지합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query(value = "DELETE FROM comment_closure WHERE descendant_id = :commentId OR ancestor_id = :commentId", nativeQuery = true)
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

