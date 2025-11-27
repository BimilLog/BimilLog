package jaeik.bimillog.domain.friend.entity;

import lombok.Getter;

import java.time.Instant;

@Getter
public class Friend {
    private final Long friendshipId;
    private final Long friendMemberId;
    private final Instant friendshipCreatedAt;
    private String memberName;
    private String thumbnailImage;

    public Friend(Long friendshipId, Long friendMemberId, Instant friendshipCreatedAt) {
        this.friendshipId = friendshipId;
        this.friendMemberId = friendMemberId;
        this.friendshipCreatedAt = friendshipCreatedAt;
    }

    public void updateInfo(FriendInfo friendInfo) {
        this.memberName = friendInfo.memberName();
        this.thumbnailImage = friendInfo.thumbnailImage();
    }

    public record FriendInfo(Long memberId, String memberName, String thumbnailImage) {}
}

