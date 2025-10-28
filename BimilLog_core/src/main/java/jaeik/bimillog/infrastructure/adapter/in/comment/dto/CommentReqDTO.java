package jaeik.bimillog.infrastructure.adapter.in.comment.dto;

import jakarta.validation.constraints.AssertTrue;
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
 * 닉네임은 최대 8글자, 내용은 사용자 입력 기준 최대 255자까지 입력 가능합니다.
 * 서버는 줄바꿈 등 특수문자 처리를 위해 최대 1000자까지 수용합니다.
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

    private Long memberId;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다.")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다.")
    private Integer password;

    @AssertTrue(message = "댓글 작성 시 게시글 ID와 내용은 필수입니다.")
    public boolean isWriteValid() {
        if (postId != null && id == null && parentId == null) {
            return content != null && !content.trim().isEmpty();
        }
        return true;
    }


    @AssertTrue(message = "댓글 수정 시 댓글 ID와 내용은 필수입니다.")
    public boolean isUpdateValid() {
        // 수정/삭제 요청 형태인지 확인 (id만 있고 postId, parentId 없음)
        boolean isModifyRequest = id != null && postId == null && parentId == null;

        if (isModifyRequest && content != null) {
            // 수정 요청인 경우: 내용이 비어있지 않아야 함
            return !content.trim().isEmpty();
        }

        // 작성 요청이거나 삭제 요청인 경우 통과
        return true;
    }

    @AssertTrue(message = "댓글 삭제 시 댓글 ID는 필수입니다.")
    public boolean isDeleteValid() {
        if (postId == null && content == null) {
            return id != null;
        }
        return true;
    }

    @AssertTrue(message = "대댓글 작성 시 부모 댓글 ID가 필수입니다.")
    public boolean isReplyValid() {
        if (parentId != null) {
            return postId != null && content != null && !content.trim().isEmpty();
        }
        return true;
    }
}
