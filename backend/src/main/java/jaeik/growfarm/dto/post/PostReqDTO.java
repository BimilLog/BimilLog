package jaeik.growfarm.dto.post;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>게시글 작성/수정/삭제 요청 DTO</h2>
 * <p>
 * 게시글 작성/수정/삭제 시 필요한 정보를 담는 DTO
 * </p>
 * <p>
 * 닉네임은 최대 8글자, 제목은 최대 30자, 내용은 최대 1000자까지 입력 가능하다.
 * </p>
 *
 * @author Jaeik
 * @version  2.0.0
 */
@Getter
@Setter
@Builder
public class PostReqDTO {

    private Long userId;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    @Size(max = 30, message = "글 제목은 최대 30자 까지 입력 가능합니다.")
    private String title;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다.")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다.")
    private Integer password;
}
