package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.CommentClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

    Optional<List<CommentClosure>> findByDescendantId(Long id);

    // 특정 댓글이 descendant인 모든 관계 삭제 (해당 댓글과 모든 조상과의 관계 삭제)
    @Modifying
    @Query("DELETE FROM CommentClosure cc WHERE cc.descendant.id = :commentId")
    void deleteByDescendantId(@Param("commentId") Long commentId);

    // 특정 댓글의 자손 댓글 존재 여부 확인
    @Query("SELECT COUNT(cc) > 0 FROM CommentClosure cc WHERE cc.ancestor.id = :commentId AND cc.descendant.id != :commentId")
    boolean hasDescendants(@Param("commentId") Long commentId);

    // 직접 부모-자식 관계 조회 (depth = 1)
    @Query("""
            SELECT cc.descendant.id, cc.ancestor.id
            FROM CommentClosure cc
            WHERE cc.descendant.id IN :commentIds
            AND cc.depth = 1
            """)
    List<Object[]> findParentChildPairs(@Param("commentIds") List<Long> commentIds);
}
