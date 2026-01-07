package jaeik.bimillog.domain.member.listener;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.event.NewMemberLoginEvent;
import jaeik.bimillog.domain.member.service.MemberOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewMemberLoginListener {
    private final MemberOnboardingService memberOnboardingService;

    @EventListener
    public void SaveMemberInfoToSession(NewMemberLoginEvent newMemberLoginEvent) {
        SocialMemberProfile memberProfile = newMemberLoginEvent.getSocialMemberProfile();
        String uuid = newMemberLoginEvent.getUuid();
        memberOnboardingService.storePendingMember(memberProfile, uuid);
    }
}
