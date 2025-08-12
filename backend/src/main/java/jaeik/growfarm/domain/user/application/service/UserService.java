package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.out.SaveBlacklistPort;
import jaeik.growfarm.domain.user.entity.BlackList;
import jaeik.growfarm.global.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 서비스</h2>
 * <p>사용자 관련 이벤트를 처리하는 서비스 클래스입니다.</p>
 * <p>사용자가 차단되었을 때 블랙리스트에 추가하는 기능을 제공합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final SaveBlacklistPort saveBlacklistPort;

    /**
     * <h3>사용자 차단 이벤트 처리</h3>
     * <p>사용자가 차단되었을 때 블랙리스트에 추가하는 메서드입니다.</p>
     *
     * @param event 차단된 사용자 이벤트
     * @since 2.0.0
     * @author jaeik
     */
    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        log.info("사용자 차단 이벤트 처리: 소셜 ID={}, 제공자={}", event.getSocialId(), event.getProvider());
        BlackList blackList = BlackList.createBlackList(event.getSocialId(), event.getProvider());
        saveBlacklistPort.save(blackList);
    }
}
