package jaeik.bimillog.infrastructure.adapter.comment.out.comment;

import jaeik.bimillog.domain.comment.application.port.out.CommentLikePort;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 추천 어댑터</h2>
 * <p>
 * 헥사고날 아키텍처의 Secondary Adapter로서 CommentLikePort 인터페이스를 구현합니다.
 * </p>
 * <p>
 * JPA Repository를 사용하여 댓글 추천 엔티티의 저장, 삭제, 조회 작업을 수행합니다.
 * </p>
 * <p>
 * 이 어댑터가 존재하는 이유: 댓글 추천 기능은 댓글 도메인의 핵심 기능이지만,
 * 별도의 엔티티와 비즈니스 로직을 가지므로 단일 책임 원칙에 따라 분리하였습니다.
 * </p>
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
     * <p>주어진 댓글 추천 엔티티를 JPA로 저장합니다.</p>
     * <p>CommentLikeUseCase가 댓글 추천 요청 시 호출합니다.</p>
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
     * <p>주어진 댓글 ID와 사용자 ID로 추천 관계를 JPA로 삭제합니다.</p>
     * <p>CommentUnlikeUseCase가 댓글 추천 취소 요청 시 호출합니다.</p>
     *
     * @param commentId 추천을 삭제할 댓글 ID
     * @param userId    추천을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteLikeByIds(Long commentId, Long userId) {
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    }

    /**
     * <h3>사용자가 댓글에 추천을 눌렀는지 여부 확인</h3>
     * <p>주어진 댓글과 사용자가 이미 추천 관계인지 JPA EXISTS 쿼리로 확인합니다.</p>
     * <p>CommentQueryUseCase가 댓글 조회 시 사용자의 추천 상태를 확인하기 위해 호출합니다.</p>
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
