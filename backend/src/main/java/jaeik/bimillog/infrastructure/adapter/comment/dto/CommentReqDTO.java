package jaeik.bimillog.infrastructure.adapter.comment.dto;

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

    @AssertTrue(message = "댓글 작성 시 게시글 ID와 내용은 필수입니다.")
    public boolean isWriteValid() {
        if (postId != null && id == null && parentId == null) {
            return content != null && !content.trim().isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "익명 댓글은 비밀번호가 필수이며, 회원 댓글은 비밀번호가 없어야 합니다.")
    public boolean isPasswordValid() {
        // 댓글 작성시에만 검증 (postId가 있는 경우)
        if (postId != null) {
            // 패스워드가 명시적으로 제공된 경우에만 익명 댓글로 간주하여 검증
            if (password != null) {
                return password >= 1000 && password <= 9999;
            }
            // userId가 명시적으로 설정되고 password가 없는 경우 회원 댓글로 검증
            if (userId != null) {
                return true;
            }
            // userId도 password도 없는 경우는 컨트롤러에서 설정할 예정이므로 통과
            return true;
        }
        return true; // 수정/삭제시에는 userId 검증 생략 (컨트롤러에서 설정됨)
    }

    @AssertTrue(message = "댓글 수정 시 댓글 ID와 내용은 필수입니다.")
    public boolean isUpdateValid() {
        if (id != null && postId == null && parentId == null && content != null) {
            return !content.trim().isEmpty();
        }
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
