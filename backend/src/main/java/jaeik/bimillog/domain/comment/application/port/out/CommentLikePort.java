package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.user.entity.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 추천 어댑터</h2>
 * <p>댓글 추천 엔티티 담당 아웃바운드 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentLikePort {

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

    //TODO 중복 메서드 리팩토링 필요
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
     * <h3>댓글 추천 삭제</h3>
     * <p>주어진 댓글 ID와 사용자 ID로 추천 관계를 삭제합니다.</p>
     *
     * @param commentId 추천을 삭제할 댓글 ID
     * @param userId    추천을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    void deleteLikeByIds(Long commentId, Long userId);

    /**
     * <h3>사용자가 댓글에 추천을 눌렀는지 여부 확인</h3>
     * <p>주어진 댓글과 사용자가 이미 추천 관계인지 확인합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return boolean 추천을 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isLikedByUser(Long commentId, Long userId);
}