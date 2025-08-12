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

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final SaveBlacklistPort saveBlacklistPort;

    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        log.info("User (Social ID: {}, Provider: {}) banned event received. Adding to blacklist.",
                event.getSocialId(), event.getProvider());
        BlackList blackList = BlackList.createBlackList(event.getSocialId(), event.getProvider());
        saveBlacklistPort.save(blackList);
    }
}
