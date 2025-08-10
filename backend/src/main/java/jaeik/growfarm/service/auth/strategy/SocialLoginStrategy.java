package jaeik.growfarm.service.auth.strategy;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.user.SocialProvider;

public interface SocialLoginStrategy {

    record LoginResult(SocialLoginUserData userData, TokenDTO tokenDTO) {
    }

    LoginResult login(String code);

    void unlink(String socialId);

    void logout(String accessToken);

    SocialProvider getProvider();
}
