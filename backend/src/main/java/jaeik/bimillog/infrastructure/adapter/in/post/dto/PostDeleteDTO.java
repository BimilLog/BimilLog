package jaeik.bimillog.infrastructure.adapter.in.post.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDeleteDTO {

    @Pattern(regexp = "^[1-9]\\d{3}$", message = "비밀번호는 1000-9999 범위의 4자리 숫자여야 합니다")
    private String password;
}
