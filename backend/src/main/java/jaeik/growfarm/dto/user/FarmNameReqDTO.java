package jaeik.growfarm.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FarmNameReqDTO {

    private Long tokenId;

    private String farmName;
}