package jaeik.growfarm.global.event;

import jaeik.growfarm.entity.user.SocialProvider;
import lombok.Getter;

@Getter
public class UserBannedEvent {

    private final Long userId;
    private final String socialId;
    private final SocialProvider provider;

    public UserBannedEvent(Long userId, String socialId, SocialProvider provider) {
        this.userId = userId;
        this.socialId = socialId;
        this.provider = provider;
    }
}
