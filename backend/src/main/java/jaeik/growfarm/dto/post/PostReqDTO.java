package jaeik.growfarm.dto.post;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>게시글 작성 요청 DTO</h2>
 * <p>
 * 게시글 작성 시 필요한 정보를 담는 DTO
 * </p>
 * <p>
 * 닉네임은 최대 8글자, 제목은 최대 30자, 내용은 최대 1000자까지 입력 가능하다.
 * </p>
 *
 * @author Jaeik
 * @version  1.0.0
 */
@Getter
@Setter
public class PostReqDTO {

    private Long userId;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    @Size(max = 30, message = "글 제목은 최대 30자 까지 입력 가능합니다.")
    private String title;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;

    @Size(max = 8, message = "비밀번호는 최대 8글자 까지 입력 가능합니다.")
    private Integer password;
}
