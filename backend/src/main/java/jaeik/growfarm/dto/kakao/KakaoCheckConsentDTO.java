package jaeik.growfarm.dto.kakao;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class KakaoCheckConsentDTO {

    private Long id;

    private KakaoCheckConsentDetailDTO[] scopes;
}
