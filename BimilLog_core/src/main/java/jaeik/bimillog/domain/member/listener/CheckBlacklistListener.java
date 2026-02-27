package jaeik.bimillog.domain.member.listener;

import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>블랙리스트 체크 이벤트 리스너</h2>
 * <p>사용자 간 블랙리스트 여부를 확인하는 이벤트를 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, level = Log.LogLevel.DEBUG, message = "블랙리스트 체크")
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
