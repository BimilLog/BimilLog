package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.comment.commentclosure;

import jaeik.growfarm.domain.comment.entity.CommentClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 클로저 레포지토리 인터페이스</h2>
 * <p>
 * 댓글의 계층 구조를 관리하는 클로저 엔티티(`CommentClosure`)의 데이터베이스 작업을 처리하는 레포지토리 인터페이스
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
     *
     * @param descendantId 자손 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 댓글 클로저 엔티티 목록. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<List<CommentClosure>> findByDescendantId(Long descendantId);

    /**
     * <h3>댓글의 자손 존재 여부 확인</h3>
     * <p>주어진 댓글 ID를 조상으로 하는 자손 댓글이 존재하는지 확인합니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean 자손 댓글이 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT COUNT(cc) > 0 FROM CommentClosure cc WHERE cc.ancestor.id = :commentId AND cc.depth > 0")
    boolean hasDescendants(@Param("commentId") Long commentId);

    /**
     * <h3>자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentId 자손 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM CommentClosure WHERE descendant.id = :commentId")
    void deleteByDescendantId(@Param("commentId") Long commentId);
}













