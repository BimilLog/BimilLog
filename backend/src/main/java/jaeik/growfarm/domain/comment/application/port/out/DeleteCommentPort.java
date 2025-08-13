package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.user.entity.User;

/**
 * <h2>댓글 삭제 포트</h2>
 * <p>댓글 엔티티 삭제를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface DeleteCommentPort {

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
     * <h3>ID로 댓글 삭제</h3>
     * <p>주어진 ID의 댓글을 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteById(Long commentId);

    /**
     * <h3>댓글 좋아요 삭제</h3>
     * <p>주어진 댓글과 사용자의 좋아요 관계를 삭제합니다.</p>
     *
     * @param comment 좋아요를 삭제할 댓글 엔티티
     * @param user    좋아요를 삭제할 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteLike(Comment comment, User user);

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByPostId(Long postId);
}
