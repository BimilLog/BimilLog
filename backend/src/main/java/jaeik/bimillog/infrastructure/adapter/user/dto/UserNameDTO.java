package jaeik.bimillog.infrastructure.adapter.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserNameDTO {

    private Long userId;

    @Size(max = 8, message = "닉네임 은 최대 8글자 까지 입력 가능합니다.")
    private String userName;
}
