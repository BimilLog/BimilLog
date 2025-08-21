package jaeik.growfarm.infrastructure.adapter.comment.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>댓글 작성/수정/삭제 요청 DTO</h2>
 * <p>
 * 댓글 작성/수정/삭제 시 필요한 정보를 담는 DTO
 * </p>
 * <p>
 * 닉네임은 최대 8글자, 내용은 최대 255자까지 입력 가능하다.
 * </p>
 * <p>
 * 비밀번호는 4자리 숫자로 입력해야 하며, 1000이상 9999 이하의 값이어야 합니다.
 * 비밀번호는 비회원 일때만 사용되며, 회원은 비밀번호 없이 댓글을 작성할 수 있습니다.
 * </p>
 *
 * @author Jaeik
 * @version  2.0.0
 */
@Getter
@Setter
public class CommentReqDTO {

    private Long id;

    private Long parentId;

    private Long postId;

    private Long userId;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다.")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다.")
    private Integer password;
}
