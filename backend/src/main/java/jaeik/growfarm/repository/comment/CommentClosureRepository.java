package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.CommentClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 계층 관계 저장소</h2>
 * <p>
 * 댓글 간의 계층 관계를 관리하는 저장소 인터페이스입니다.
 * </p>
 * <p>
 * 댓글의 조상과 자손 관계를 저장하고 조회하는 기능을 제공합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

    /**
     * <h3>조상 댓글 조회</h3>
     * <p>
     * 특정 댓글의 조상 댓글을 조회합니다.
     * </p>
     *
     * @param id 댓글 ID
     * @return 조상 댓글 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    Optional<List<CommentClosure>> findByDescendantId(Long descendantId);

    /**
     * <h3>댓글 클로저 관계 삭제</h3>
     * <p>
     * 특정 댓글과 관련된 모든 클로저 테이블 관계를 삭제합니다.
     * </p>
     * <p>
     * 댓글 하드 삭제 시 사용됩니다.
     * </p>
     *
     * @param commentId 삭제할 댓글 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Modifying
    @Query("DELETE FROM CommentClosure cc WHERE cc.descendant.id = :commentId")
    void deleteByDescendantId(@Param("commentId") Long commentId);

    /**
     * <h3>자손 댓글 존재 여부 확인</h3>
     * <p>
     * 특정 댓글이 자손 댓글(답글)을 가지고 있는지 확인합니다.
     * </p>
     * <p>
     * 댓글 삭제 시 소프트 삭제 여부를 결정하는데 사용됩니다.
     * </p>
     *
     * @param commentId 확인할 댓글 ID
     * @return 자손 댓글이 존재하면 true, 없으면 false
     * @since 2.0.0
     * @author Jaeik
     */
    @Query("SELECT COUNT(cc) > 1 FROM CommentClosure cc WHERE cc.ancestor.id = :commentId")
    boolean hasDescendants(@Param("commentId") Long commentId);

    /**
     * <h3>댓글 ID 리스트로 댓글관계 삭제</h3>
     * <p>
     * 특정 댓글 ID 리스트에 해당하는 모든 댓글 관계를 삭제합니다.
     * </p>
     *
     * @param commentIds 삭제할 댓글 ID 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Modifying
    @Query("DELETE FROM CommentClosure cc WHERE cc.descendant.id IN :commentIds")
    void deleteByDescendantIds(@Param("commentIds") List<Long> commentIds);
}
