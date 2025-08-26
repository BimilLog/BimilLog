package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.domain.comment.entity.CommentRequest;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;

public interface CommentLikeUseCase {

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>댓글에 추천를 누르거나 취소합니다.</p>
     *
     * @param commentRequest  추천/취소할 댓글 요청 (ID만 사용)
     * @param userDetails 사용자 인증 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void likeComment(CommentRequest commentRequest, CustomUserDetails userDetails);
}
