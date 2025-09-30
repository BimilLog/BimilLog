package jaeik.bimillog.infrastructure.adapter.out.api.dto;

import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KakaoUserInfoDTO {
    private final String socialId;
    private final String email;
    private final SocialProvider provider;
    private final String nickname;
    private final String profileImageUrl;

    public static KakaoUserInfoDTO of(
            String socialId,
            String email,
            SocialProvider provider,
            String nickname,
            String profileImageUrl
    ) {
        return new KakaoUserInfoDTO(socialId, email, provider, nickname, profileImageUrl);
    }
}
