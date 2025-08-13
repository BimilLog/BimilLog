package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentLike;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>댓글 좋아요 조회 포트</h2>
 * <p>댓글 좋아요 엔티티 조회를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadCommentLikePort {

    /**
     * <h3>댓글 ID와 사용자 ID로 댓글 좋아요 조회</h3>
     * <p>주어진 댓글 ID와 사용자 ID에 해당하는 댓글 좋아요 엔티티를 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return Optional<CommentLike> 조회된 댓글 좋아요 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * <h3>여러 댓글 ID에 대한 좋아요 수 조회</h3>
     * <p>주어진 댓글 ID 목록에 해당하는 각 댓글의 좋아요 수를 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @return Map<Long, Long> 댓글 ID를 키로, 좋아요 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Long> countByCommentIds(List<Long> commentIds);
}
