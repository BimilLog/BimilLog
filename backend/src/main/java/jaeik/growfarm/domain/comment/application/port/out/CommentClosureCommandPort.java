package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentClosure;

/**
 * <h2>댓글 클로저 명령 포트</h2>
 * <p>댓글 클로저 엔티티 생성/수정/삭제를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 명령 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentClosureCommandPort {

    /**
     * <h3>댓글 클로저 저장</h3>
     * <p>주어진 댓글 클로저 엔티티를 저장합니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void save(CommentClosure commentClosure);

    /**
     * <h3>댓글 클로저 삭제</h3>
     * <p>주어진 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentClosure 삭제할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void delete(CommentClosure commentClosure);

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
    void deleteByDescendantIds(java.util.List<Long> commentIds);
}