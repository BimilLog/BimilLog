package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentclosure;

import jaeik.growfarm.domain.comment.application.port.out.CommentClosureCommandPort;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentClosureCommandAdapter implements CommentClosureCommandPort {

    private final CommentClosureRepository commentClosureRepository;

    /**
     * <h3>댓글 클로저 저장</h3>
     * <p>주어진 댓글 클로저 엔티티를 저장합니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(CommentClosure commentClosure) {
        commentClosureRepository.save(commentClosure);
    }

    /**
     * <h3>댓글 클로저 삭제</h3>
     * <p>주어진 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentClosure 삭제할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(CommentClosure commentClosure) {
        commentClosureRepository.delete(commentClosure);
    }

    /**
     * <h3>자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentId 자손 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByDescendantId(Long commentId) {
        commentClosureRepository.deleteByDescendantId(commentId);
    }
}
