package jaeik.growfarm.dto.auth;

import jaeik.growfarm.entity.user.SocialProvider;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SocialLoginUserData {

    private final String socialId;
    private final SocialProvider provider;
    private final String nickname;
    private final String profileImageUrl;

    @Builder
    public SocialLoginUserData(String socialId, SocialProvider provider, String nickname, String profileImageUrl) {
        this.socialId = socialId;
        this.provider = provider;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}
