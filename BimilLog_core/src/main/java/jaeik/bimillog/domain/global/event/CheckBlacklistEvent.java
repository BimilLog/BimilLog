package jaeik.bimillog.domain.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckBlacklistEvent {
    private Long memberId;
    private Long targetMemberId;
}
