package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.commentlike;

import jaeik.growfarm.domain.comment.application.port.out.CommentLikeQueryPort;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentLikeQueryAdapter implements CommentLikeQueryPort {

    private final CommentLikeRepository commentLikeRepository;

    /**
     * <h3>댓글 ID와 사용자 ID로 댓글 추천 조회</h3>
     * <p>주어진 댓글 ID와 사용자 ID에 해당하는 댓글 추천 엔티티를 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return Optional<CommentLike> 조회된 댓글 추천 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId) {
        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
    }

    /**
     * <h3>여러 댓글 ID에 대한 추천 수 조회</h3>
     * <p>주어진 댓글 ID 목록에 해당하는 각 댓글의 추천 수를 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @return Map<Long, Long> 댓글 ID를 키로, 추천 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<Long, Long> countByCommentIds(List<Long> commentIds) {
        return commentLikeRepository.countByCommentIds(commentIds);
    }

    /**
     * <h3>사용자가 댓글에 추천를 눌렀는지 여부 확인</h3>
     * <p>주어진 댓글과 사용자가 이미 추천 관계인지 확인합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return boolean 추천를 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isLikedByUser(Long commentId, Long userId) {
        // 이 로직은 userId와 commentId만으로 처리하는 것이 더 효율적일 수 있습니다.
        // 현재는 CommentService에서 이미 User와 Comment 엔티티를 조회하고 있어,
        // 그 엔티티를 활용하는 것이 추가적인 쿼리를 줄일 수 있습니다.
        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId).isPresent();
    }
}
