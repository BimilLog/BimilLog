package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.CommentClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

    Optional<List<CommentClosure>> findByDescendantId(Long id);
}
