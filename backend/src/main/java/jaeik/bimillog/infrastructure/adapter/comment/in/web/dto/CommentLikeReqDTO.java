package jaeik.bimillog.infrastructure.adapter.comment.in.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>댓글 좋아요 요청 DTO</h2>
 * <p>
 * 댓글 좋아요/좋아요 취소 시 필요한 최소한의 정보를 담는 DTO
 * </p>
 * <p>
 * 기존 CommentReqDTO와 달리 좋아요 기능에만 필요한 commentId만 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class CommentLikeReqDTO {

    @NotNull(message = "댓글 ID는 필수입니다.")
    private Long commentId;
}