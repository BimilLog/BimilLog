package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentClosure;

/**
 * <h2>댓글 클로저 저장 포트</h2>
 * <p>댓글 클로저 엔티티 저장을 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SaveCommentClosurePort {
    /**
     * <h3>댓글 클로저 저장</h3>
     * <p>주어진 댓글 클로저 엔티티를 저장합니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void save(CommentClosure commentClosure);
}
