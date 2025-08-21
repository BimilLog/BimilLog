package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentlike;

import jaeik.growfarm.domain.comment.application.port.out.CommentLikeQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * <h2>댓글 추천 조회 어댑터</h2>
 * <p>댓글 추천 엔티티 조회를 위한 Out-Port 구현체</p>
 * <p>CQRS 패턴에 따른 조회 전용 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentLikeQueryAdapter implements CommentLikeQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

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
    public boolean isLikedByUser(@Param("commentId") Long commentId, @Param("userId") Long userId) {
        String jpql = "SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.user.id = :userId";
        return entityManager.createQuery(jpql, Boolean.class)
                .setParameter("commentId", commentId)
                .setParameter("userId", userId)
                .getSingleResult();

    }
}
