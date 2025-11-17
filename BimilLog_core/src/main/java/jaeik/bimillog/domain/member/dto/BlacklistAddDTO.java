package jaeik.bimillog.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BlacklistAddDTO {

    @NotBlank(message = "사용자 이름은 필수입니다.")
    @Size(min = 1, max = 8, message = "사용자 이름은 1자 이상 8자 이하여야 합니다.")
    private String memberName;
}
