package jaeik.growfarm.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostSetAsNoticeEvent {
    private final Long postId;
}
