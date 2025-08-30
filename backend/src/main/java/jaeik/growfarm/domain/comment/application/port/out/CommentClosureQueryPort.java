package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentClosure;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 클로저 조회 포트</h2>
 * <p>댓글 클로저 엔티티 조회를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 조회 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentClosureQueryPort {

    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID에 해당하는 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
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
    boolean hasDescendants(Long commentId);

    /**
     * <h3>최적화된 댓글 삭제 여부 확인</h3>
     * <p>댓글의 자손 존재 여부를 확인하여 삭제 가능 여부를 반환합니다.</p>
     * <p>자손이 있으면 소프트 삭제, 없으면 하드 삭제가 가능함을 나타냅니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean true면 하드 삭제 가능(자손 없음), false면 소프트 삭제 필요(자손 있음)
     * @author Jaeik
     * @since 2.0.0
     */
    boolean canHardDelete(Long commentId);
}