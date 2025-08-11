package jaeik.growfarm.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostUnsetAsNoticeEvent {
    private final Long postId;
}
