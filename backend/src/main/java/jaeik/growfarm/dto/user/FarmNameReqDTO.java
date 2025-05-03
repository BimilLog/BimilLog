package jaeik.growfarm.dto.user;

import lombok.Getter;
import lombok.Setter;

// 회원 가입시 농장 이름 등록할 때 전달 받는 DTO
@Getter
@Setter
public class FarmNameReqDTO {

    private Long tokenId;

    private String farmName;
}