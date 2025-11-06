package jaeik.bimillog.domain.post.in.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDeleteDTO {

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다")
    private Integer password;
}
