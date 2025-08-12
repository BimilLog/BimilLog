package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.comment.domain.CommentClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

    Optional<List<CommentClosure>> findByDescendantId(Long descendantId);

    @Query("SELECT COUNT(cc) > 0 FROM CommentClosure cc WHERE cc.ancestor.id = :commentId AND cc.depth > 0")
    boolean hasDescendants(@Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM CommentClosure WHERE descendant.id = :commentId")
    void deleteByDescendantId(@Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM CommentClosure WHERE descendant.id IN :commentIds")
    void deleteByDescendantIds(@Param("commentIds") List<Long> commentIds);
}









