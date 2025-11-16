package jaeik.bimillog.domain.friend.entity;

import java.time.Instant;
import java.util.List;

public class Friends {

    private Long memberId;
    private List<Friend> friendList;

    static class Friend {
        private Long friendId;
        private Instant modifiedAt;
    }
}
