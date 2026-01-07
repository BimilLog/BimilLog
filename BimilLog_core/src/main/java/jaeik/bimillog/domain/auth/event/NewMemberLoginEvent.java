package jaeik.bimillog.domain.auth.event;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NewMemberLoginEvent {
    private SocialMemberProfile socialMemberProfile;
    private String uuid;
}
