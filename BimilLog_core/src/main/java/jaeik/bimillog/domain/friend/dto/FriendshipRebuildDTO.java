package jaeik.bimillog.domain.friend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class FriendshipRebuildDTO {
    Long memberId;
    Set<Long> friendIds;

    public static FriendshipRebuildDTO createDTO(Long memberId, Set<Long> friendIds) {
        return builder().memberId(memberId).friendIds(friendIds).build();
    }
}
