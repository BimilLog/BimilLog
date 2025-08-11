package jaeik.growfarm.dto.auth;

import jaeik.growfarm.dto.user.TokenDTO;
import lombok.Getter;

@Getter
public class TemporaryUserDataDTO {
    private final SocialLoginUserData socialLoginUserData;
    private final TokenDTO tokenDTO;
    private final String fcmToken;

    public TemporaryUserDataDTO(SocialLoginUserData socialLoginUserData, TokenDTO tokenDTO, String fcmToken) {
        this.socialLoginUserData = socialLoginUserData;
        this.tokenDTO = tokenDTO;
        this.fcmToken = fcmToken;
    }
}
