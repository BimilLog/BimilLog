package jaeik.growfarm.infrastructure.adapter.auth.out.social.dto;

import jaeik.growfarm.domain.user.entity.TokenVO;
import lombok.Getter;

@Getter
public class TemporaryUserDataDTO {
    public SocialLoginUserData socialLoginUserData;
    public TokenVO tokenVO;
    public String fcmToken;

    public TemporaryUserDataDTO(SocialLoginUserData socialLoginUserData, TokenVO tokenVO, String fcmToken) {
        this.socialLoginUserData = socialLoginUserData;
        this.tokenVO = tokenVO;
        this.fcmToken = fcmToken;
    }
}
