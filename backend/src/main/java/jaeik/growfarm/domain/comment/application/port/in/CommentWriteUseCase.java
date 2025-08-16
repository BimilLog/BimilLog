package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;

public interface CommentWriteUseCase {

    /**
     * <h3>댓글 작성</h3>
     * <p>새로운 댓글을 작성합니다.</p>
     *
     * @param userDetails 사용자 인증 정보
     * @param commentDto  댓글 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    void writeComment(CustomUserDetails userDetails, CommentDTO commentDto);
}
