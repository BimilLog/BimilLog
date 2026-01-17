package jaeik.bimillog.domain.member.listener;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.event.NewMemberLoginEvent;
import jaeik.bimillog.domain.member.service.MemberOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>신규 회원 로그인 이벤트 리스너</h2>
 * <p>신규 회원 로그인 시 세션에 회원 정보를 저장하는 이벤트를 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "신규 회원 로그인 이벤트")
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
