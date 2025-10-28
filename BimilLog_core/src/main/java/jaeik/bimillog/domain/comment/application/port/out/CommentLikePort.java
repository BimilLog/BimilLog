package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
import jaeik.bimillog.domain.comment.entity.CommentLike;

/**
 * <h2>댓글 추천 포트</h2>
 * <p>댓글 추천 시스템을 담당하는 포트입니다.</p>
 * <p>추천 정보 저장, 추천 취소, 추천 상태 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentLikePort {

    /**
     * <h3>댓글 추천 관계 저장</h3>
     * <p>사용자와 댓글 간의 추천 관계를 저장합니다.</p>
     * <p>{@link CommentCommandService}에서 사용자의 댓글 추천 요청을 처리할 때 호출됩니다.</p>
     *
     * @param commentLike 저장할 댓글 추천 엔티티
     * @return CommentLike 저장된 댓글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    CommentLike save(CommentLike commentLike);

    /**
     * <h3>댓글 추천 관계 삭제</h3>
     * <p>특정 사용자와 댓글 간의 추천 관계를 삭제합니다.</p>
     * <p>{@link CommentCommandService}에서 사용자의 댓글 추천 취소 요청을 처리할 때 호출됩니다.</p>
     *
     * @param commentId 추천을 삭제할 댓글 ID
     * @param memberId    추천을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteLikeByIds(Long commentId, Long memberId);

    /**
     * <h3>댓글 추천 상태 확인</h3>
     * <p>특정 사용자가 해당 댓글을 이미 추천했는지 여부를 확인합니다.</p>
     * <p>{@link CommentCommandService}에서 추천/취소 로직 분기 처리 시 호출됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @param memberId    사용자 ID
     * @return boolean 추천을 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isLikedByUser(Long commentId, Long memberId);
}