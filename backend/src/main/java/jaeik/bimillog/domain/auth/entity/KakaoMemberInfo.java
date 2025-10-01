package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KakaoMemberInfo {
    private final String socialId;
    private final String email;
    private final SocialProvider provider;
    private final String nickname;
    private final String profileImageUrl;

    public static KakaoMemberInfo of(
            String socialId,
            String email,
            SocialProvider provider,
            String nickname,
            String profileImageUrl
    ) {
        return new KakaoMemberInfo(socialId, email, provider, nickname, profileImageUrl);
    }
}
