package jaeik.growfarm.dto.auth;

import jaeik.growfarm.global.domain.SocialProvider;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SocialLoginUserData {
    private final String socialId;
    private final String email;
    private final SocialProvider provider;
    private final String nickname;
    private final String profileImageUrl;
    private final String fcmToken;

    @Builder
    public SocialLoginUserData(String socialId, SocialProvider provider, String nickname, String profileImageUrl, String fcmToken) {
        this.socialId = socialId;
        this.provider = provider;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.email = null;
        this.fcmToken = fcmToken;
    }

    // 명시적 getter 추가 (혹시 모를 Lombok 문제 대비)
    public String getFcmToken() {
        return fcmToken;
    }
}
