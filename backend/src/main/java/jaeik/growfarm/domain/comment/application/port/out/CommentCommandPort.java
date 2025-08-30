package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;

/**
 * <h2>댓글 명령 포트</h2>
 * <p>댓글 엔티티 생성/수정/삭제를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 명령 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentCommandPort {

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
     * <h3>댓글 삭제</h3>
     * <p>주어진 댓글 엔티티를 삭제합니다.</p>
     *
     * @param comment 삭제할 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void delete(Comment comment);

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByPostId(Long postId);

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void anonymizeUserComments(Long userId);

    /**
     * <h3>최적화된 댓글 삭제</h3>
     * <p>자손 존재 여부에 따라 자동으로 소프트/하드 삭제를 수행합니다.</p>
     * <p>단일 쿼리로 자손 체크와 삭제를 통합하여 성능을 최적화합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return boolean true면 하드 삭제 수행됨, false면 소프트 삭제 수행됨
     * @author Jaeik
     * @since 2.0.0
     */
    boolean deleteCommentOptimized(Long commentId);
}