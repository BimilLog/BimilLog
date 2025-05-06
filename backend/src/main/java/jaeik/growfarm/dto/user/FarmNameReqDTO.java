package jaeik.growfarm.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 회원 가입시 농장 이름 등록할 때 전달 받는 DTO
@Getter
@Setter
public class FarmNameReqDTO {

    private Long tokenId;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;
}