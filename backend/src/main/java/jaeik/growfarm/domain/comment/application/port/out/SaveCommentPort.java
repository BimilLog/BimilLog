package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;

/**
 * <h2>댓글 저장 포트</h2>
 * <p>댓글 엔티티 저장을 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SaveCommentPort {

    /**
     * <h3>댓글 저장</h3>
     * <p>주어진 댓글 엔티티를 저장합니다.</p>
     *
     * @param comment 저장할 댓글 엔티티
     * @return Comment 저장된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Comment save(Comment comment);

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void anonymizeUserComments(Long userId);
}
