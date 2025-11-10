package jaeik.bimillog.infrastructure.api.social.naver;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.global.strategy.SocialAuthStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;

public class NaverAuthStrategy implements SocialAuthStrategy {

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.NAVER;
    }

    @Override
    public SocialMemberProfile getSocialToken(String code) {
        return null;
    }

    @Override
    public void getUserInfo(String accessToken) {

    }

    @Override
    public void unlink(String socialId) {

    }

    @Override
    public void logout(String accessToken) throws Exception {

    }

    @Override
    public void forceLogout(String socialId) {

    }
}
