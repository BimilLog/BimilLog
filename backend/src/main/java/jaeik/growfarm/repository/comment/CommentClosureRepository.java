package jaeik.growfarm.repository.comment;

import jaeik.growfarm.entity.comment.CommentClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

}
