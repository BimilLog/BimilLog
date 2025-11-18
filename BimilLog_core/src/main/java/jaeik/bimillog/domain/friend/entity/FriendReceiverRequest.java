package jaeik.bimillog.domain.friend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendReceiverRequest {
    private Long friendRequestId;
    private Long senderMemberId;
    private String senderMemberName;
}
