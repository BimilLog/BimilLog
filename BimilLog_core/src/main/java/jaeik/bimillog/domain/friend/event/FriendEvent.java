package jaeik.bimillog.domain.friend.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendEvent {
    private Long receiveMemberId;
    private String sseMessage;
    private String fcmTitle;
    private String fcmBody;
}
