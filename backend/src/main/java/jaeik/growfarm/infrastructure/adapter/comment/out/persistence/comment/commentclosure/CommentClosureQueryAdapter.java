package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentclosure;

import jaeik.growfarm.domain.comment.application.port.out.CommentClosureQueryPort;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentClosureQueryAdapter implements CommentClosureQueryPort {

    private final CommentClosureRepository commentClosureRepository;

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
     *
     * @param descendantId 자손 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 댓글 클로저 엔티티 목록. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<List<CommentClosure>> findByDescendantId(Long descendantId) {
        return commentClosureRepository.findByDescendantId(descendantId);
    }

    /**
     * <h3>댓글의 자손 존재 여부 확인</h3>
     * <p>주어진 댓글 ID를 조상으로 하는 자손 댓글이 존재하는지 확인합니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean 자손 댓글이 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean hasDescendants(Long commentId) {
        return commentClosureRepository.hasDescendants(commentId);
    }
}
