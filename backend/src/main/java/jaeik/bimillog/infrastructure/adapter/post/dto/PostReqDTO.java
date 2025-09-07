package jaeik.bimillog.infrastructure.adapter.post.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h2>게시글 작성/수정 요청 DTO</h2>
 * <p>
 * 게시글 작성/수정 시 필요한 정보를 담는 DTO
 * </p>
 * <p>
 * 제목은 최대 30자, 내용은 최대 1000자까지 입력 가능하며, 비밀번호는 4자리 숫자이다.
 * </p>
 *
 * @author Jaeik
 * @version  2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReqDTO {

    @Size(max = 30, message = "글 제목은 최대 30자 까지 입력 가능합니다.")
    private String title;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;

    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다.")
    private String password;
}
