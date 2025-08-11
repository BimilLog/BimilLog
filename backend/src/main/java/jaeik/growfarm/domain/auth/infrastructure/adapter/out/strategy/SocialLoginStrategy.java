package jaeik.growfarm.domain.auth.infrastructure.adapter.out.strategy;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;

public interface SocialLoginStrategy {

    LoginResultDTO login(String code);

    void unlink(String socialId);

    void logout(String accessToken);

    SocialProvider getProvider();
}
