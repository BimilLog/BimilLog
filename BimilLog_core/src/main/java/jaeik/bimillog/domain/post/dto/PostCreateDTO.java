package jaeik.bimillog.domain.post.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h2>게시글 작성 요청 DTO</h2>
 * <p>게시글 작성 시 필요한 정보를 담는 요청 DTO입니다.</p>
 * <p>익명 게시글과 회원 게시글을 위한 검증 규칙 포함</p>
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

    // 실제로는 3000자까지 허용하지만 이는 Html등을 저장하기 위해서고 사용자에게 안내는 1000자로 함 프론트에도 1000자로 안내
    @NotBlank(message = "게시글 내용은 필수입니다")
    @Size(min = 10, max = 3000, message = "게시글 내용은 10자 이상 1000자 이하여야 합니다")
    private String content;

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다")
    private Integer password;
}
