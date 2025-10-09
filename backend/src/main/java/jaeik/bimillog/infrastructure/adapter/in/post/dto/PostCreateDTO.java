package jaeik.bimillog.infrastructure.adapter.in.post.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h2>게시글 작성 요청 DTO</h2>
 * <p>게시글 작성 시 필요한 정보를 담는 요청 DTO입니다.</p>
 * <p>익명 게시글과 회원 게시글을 위한 검증 규칙 포함</p>
 * <p>PostCommandController의 게시글 작성 API에서 요청 바디로 사용됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateDTO {

    @NotBlank(message = "게시글 제목은 필수입니다")
    @Size(min = 2, max = 30, message = "게시글 제목은 2자 이상 30자 이하여야 합니다")
    private String title;

    @NotBlank(message = "게시글 내용은 필수입니다")
    @Size(min = 10, max = 3000, message = "게시글 내용은 10자 이상 3000자 이하여야 합니다")
    private String content;

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다")
    private Integer password;
}
