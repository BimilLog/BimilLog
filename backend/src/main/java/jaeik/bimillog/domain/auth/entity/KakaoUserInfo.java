package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KakaoUserInfo {
    private final String socialId;
    private final String email;
    private final SocialProvider provider;
    private final String nickname;
    private final String profileImageUrl;

    public static KakaoUserInfo of(
            String socialId,
            String email,
            SocialProvider provider,
            String nickname,
            String profileImageUrl
    ) {
        return new KakaoUserInfo(socialId, email, provider, nickname, profileImageUrl);
    }
}
