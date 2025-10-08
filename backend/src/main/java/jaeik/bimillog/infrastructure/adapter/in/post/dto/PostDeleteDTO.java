package jaeik.bimillog.infrastructure.adapter.in.post.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDeleteDTO {

    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다")
    private String password;
}
