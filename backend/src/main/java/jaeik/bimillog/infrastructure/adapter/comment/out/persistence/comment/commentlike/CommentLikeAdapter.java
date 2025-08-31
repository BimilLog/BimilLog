package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentlike;

import jaeik.bimillog.domain.comment.application.port.out.CommentLikePort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 추천 커맨드 어댑터</h2>
 * <p>댓글 추천 엔티티의 저장 및 삭제 작업을 처리하는 Out-Port 구현체</p>
 * <p>CQRS 패턴에 따른 커맨드 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentLikeAdapter implements CommentLikePort {

    private final CommentLikeRepository commentLikeRepository;

    /**
     * <h3>댓글 추천 저장</h3>
     * <p>주어진 댓글 추천 엔티티를 저장합니다.</p>
     *
     * @param commentLike 저장할 댓글 추천 엔티티
     * @return CommentLike 저장된 댓글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public CommentLike save(CommentLike commentLike) {
        return commentLikeRepository.save(commentLike);
    }

    /**
     * <h3>댓글 추천 삭제</h3>
     * <p>주어진 댓글과 사용자의 추천 관계를 삭제합니다.</p>
     *
     * @param comment 추천를 삭제할 댓글 엔티티
     * @param user    추천를 삭제할 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    @Override
    public void deleteLike(Comment comment, User user) {
        commentLikeRepository.deleteByCommentAndUser(comment, user);
    }

    /**
     * <h3>댓글 추천 삭제 (ID 기반 최적화)</h3>
     * <p>주어진 댓글 ID와 사용자 ID로 추천 관계를 직접 삭제합니다.</p>
     * <p>성능 최적화: 엔티티 조회 없이 ID만으로 직접 삭제</p>
     *
     * @param commentId 추천을 삭제할 댓글 ID
     * @param userId    추천을 삭제할 사용자 ID
     * @return int 삭제된 행의 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    @Override
    public void deleteLikeByIds(Long commentId, Long userId) {
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    }

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
