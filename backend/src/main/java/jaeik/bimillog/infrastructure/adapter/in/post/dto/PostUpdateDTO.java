package jaeik.bimillog.infrastructure.adapter.in.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h2>게시글 수정 요청 DTO</h2>
 * <p>게시글 수정 API의 요청 데이터를 검증하는 DTO입니다.</p>
 * <p>제목과 내용의 필수 검증, 최소/최대 길이 검증</p>
 * <p>PostCommandController의 게시글 수정 API에서 사용됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateDTO {

    @NotBlank(message = "게시글 제목은 필수입니다")
    @Size(min = 2, max = 30, message = "게시글 제목은 2자 이상 30자 이하여야 합니다")
    private String title;

    @NotBlank(message = "게시글 내용은 필수입니다")
    @Size(min = 10, max = 1000, message = "게시글 내용은 10자 이상 1000자 이하여야 합니다")
    private String content;
}