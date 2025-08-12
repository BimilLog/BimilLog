package jaeik.growfarm.dto.auth;

import jaeik.growfarm.dto.user.TokenDTO;
import lombok.Getter;

@Getter
public class TemporaryUserDataDTO {
    public SocialLoginUserData socialLoginUserData;
    public TokenDTO tokenDTO;
    public String fcmToken;

    public TemporaryUserDataDTO(SocialLoginUserData socialLoginUserData, TokenDTO tokenDTO, String fcmToken) {
        this.socialLoginUserData = socialLoginUserData;
        this.tokenDTO = tokenDTO;
        this.fcmToken = fcmToken;
    }
}
