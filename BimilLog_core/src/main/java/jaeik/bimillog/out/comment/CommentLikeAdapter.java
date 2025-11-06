package jaeik.bimillog.out.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentLikePort;
import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 추천 어댑터</h2>
 * <p>댓글 추천 포트의 구현체입니다.</p>
 * <p>댓글 추천 엔티티 저장, 댓글 추천 삭제</p>
 * <p>사용자가 댓글에 추천을 눌렀는지 여부 확인</p>
 * <p>댓글 추천/취소 기능 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentLikeAdapter implements CommentLikePort {

    private final CommentLikeRepository commentLikeRepository;

    /**
     * <h3>댓글 추천 엔티티 저장</h3>
     * <p>주어진 댓글 추천 엔티티를 데이터베이스에 저장합니다.</p>
     * <p>사용자가 댓글에 추천을 눌렀는 관계를 영속적으로 저장</p>
     * <p>{@link CommentCommandService}에서 댓글 추천 처리 시 호출됩니다.</p>
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
     * <h3>댓글 추천 엔티티 삭제</h3>
     * <p>댓글 ID와 사용자 ID로 추천 관계를 삭제합니다.</p>
     * <p>사용자가 이미 추천한 댓글에 대해 추천을 취소하는 기능</p>
     * <p>{@link CommentCommandService}에서 댓글 추천 취소 처리 시 호출됩니다.</p>
     *
     * @param commentId 추천을 삭제할 댓글 ID
     * @param memberId 추천을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteLikeByIds(Long commentId, Long memberId) {
        commentLikeRepository.deleteByCommentIdAndMemberId(commentId, memberId);
    }

    /**
     * <h3>사용자가 댓글에 추천을 눌렀는지 여부 확인</h3>
     * <p>주어진 댓글과 사용자가 이미 추천 관계인지 확인합니다.</p>
     * <p>댓글 추천/취소 기능의 토글 처리를 위한 상태 확인</p>
     * <p>{@link CommentCommandService}에서 댓글 추천 상태 확인 시 호출됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @param memberId 사용자 ID
     * @return boolean 추천을 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isLikedByUser(Long commentId, Long memberId) {
        return commentLikeRepository.existsByCommentIdAndMemberId(commentId, memberId);
    }
}
