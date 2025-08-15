package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.comment.commentlike;

import jaeik.growfarm.domain.comment.application.port.out.CommentLikeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class CommentLikeQueryAdapter implements CommentLikeQueryPort {

    private final CommentLikeRepository commentLikeRepository;


    /**
     * <h3>사용자가 댓글에 추천을 눌렀는지 여부 확인 (EXISTS 최적화)</h3>
     * <p>주어진 댓글과 사용자가 이미 추천 관계인지 EXISTS 쿼리로 효율적으로 확인합니다.</p>
     * <p>성능 최적화: 기존 findByCommentIdAndUserId().isPresent() 대신 전용 EXISTS 쿼리 사용</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return boolean 추천을 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isLikedByUser(Long commentId, Long userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }
}
