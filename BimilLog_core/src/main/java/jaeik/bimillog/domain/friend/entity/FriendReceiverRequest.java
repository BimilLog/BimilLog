package jaeik.bimillog.domain.friend.entity;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendReceiverRequest {
    private Long friendRequestId;
    private Long senderMemberId;

    @Size(min = 1, max = 8, message = "사용자 이름은 1자 이상 8자 이하여야 합니다.")
    private String senderMemberName;
}
