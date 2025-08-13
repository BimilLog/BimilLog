package jaeik.growfarm.domain.comment.application.port.out;

import java.util.List;

/**
 * <h2>댓글 클로저 삭제 포트</h2>
 * <p>댓글 클로저 엔티티 삭제를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface DeleteCommentClosurePort {

    /**
     * <h3>자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentId 자손 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByDescendantId(Long commentId);

    /**
     * <h3>여러 자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 여러 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentIds 여러 자손 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByDescendantIds(List<Long> commentIds);
}
