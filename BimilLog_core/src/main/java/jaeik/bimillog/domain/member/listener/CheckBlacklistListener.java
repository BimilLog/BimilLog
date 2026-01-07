package jaeik.bimillog.domain.member.listener;

import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckBlacklistListener {
    private final MemberBlacklistService memberBlacklistService;

    @EventListener
    public void checkBlacklist(CheckBlacklistEvent checkBlacklistEvent) {
        Long memberId = checkBlacklistEvent.getMemberId();
        Long targetMemberId = checkBlacklistEvent.getTargetMemberId();
        memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId);
    }
}
