package jaeik.bimillog.infrastructure.adapter.in.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserNameDTO {

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 1, max = 8, message = "닉네임은 1자 이상 8자 이하여야 합니다")
    private String userName;
}
