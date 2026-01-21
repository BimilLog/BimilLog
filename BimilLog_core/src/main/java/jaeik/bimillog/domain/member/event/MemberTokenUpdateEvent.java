package jaeik.bimillog.domain.member.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberTokenUpdateEvent {
    private Long tokenId;
    private String newJwtRefreshToken;
}
