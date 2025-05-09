package jaeik.growfarm.dto.kakao;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class KakaoCheckConsentDetailDTO {

    private String id;
    private String display_name;
    private String type;
    private boolean using;
    private boolean agreed;
    private boolean revocable;
}
