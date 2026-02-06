package jaeik.bimillog.domain.friend.entity;

import java.time.Instant;

public record Friend(Long friendshipId, Long friendMemberId,
                     String memberName, String thumbnailImage, Instant friendshipCreatedAt) {
}

