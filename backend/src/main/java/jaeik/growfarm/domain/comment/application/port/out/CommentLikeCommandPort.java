package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.user.entity.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 추천 명령 포트</h2>
 * <p>댓글 추천 엔티티 생성/수정/삭제를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 명령 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentLikeCommandPort {

    /**
     * <h3>댓글 추천 저장</h3>
     * <p>주어진 댓글 추천 엔티티를 저장합니다.</p>
     *
     * @param commentLike 저장할 댓글 추천 엔티티
     * @return CommentLike 저장된 댓글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    CommentLike save(CommentLike commentLike);

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
    void deleteLike(Comment comment, User user);

    /**
     * <h3>댓글 추천 삭제 (ID 기반 최적화)</h3>
     * <p>주어진 댓글 ID와 사용자 ID로 추천 관계를 삭제합니다.</p>
     * <p>성능 최적화: 엔티티 조회 없이 ID만으로 직접 삭제</p>
     *
     * @param commentId 추천을 삭제할 댓글 ID
     * @param userId    추천을 삭제할 사용자 ID
     * @return int 삭제된 행의 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    int deleteLikeByIds(Long commentId, Long userId);
}