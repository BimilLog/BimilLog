package jaeik.bimillog.domain.friend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendSenderRequest {
    private Long friendRequestId;
    private Long receiverMemberId;
    private String receiverMemberName;
}
