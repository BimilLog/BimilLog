package jaeik.growfarm.global.event;

import lombok.Getter;

@Getter
public class UserBannedEvent {

    private final Long userId;
    private final Long kakaoId;

    public UserBannedEvent(Long userId, Long kakaoId) {
        this.userId = userId;
        this.kakaoId = kakaoId;
    }
}
