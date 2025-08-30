package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentclosure;

import jaeik.growfarm.domain.comment.application.port.out.CommentClosureQueryPort;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 클로저 조회 어댑터</h2>
 * <p>댓글 클로저 엔티티 조회를 위한 Out-Port 구현체</p>
 * <p>CQRS 패턴에 따른 조회 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
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

    /**
     * <h3>댓글 삭제 여부 확인</h3>
     * <p>댓글의 자손 존재 여부를 확인하여 삭제 가능 여부를 반환합니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean true면 하드 삭제 가능(자손 없음), false면 소프트 삭제 필요(자손 있음)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean canHardDelete(Long commentId) {
        return !commentClosureRepository.hasDescendants(commentId);
    }
}
