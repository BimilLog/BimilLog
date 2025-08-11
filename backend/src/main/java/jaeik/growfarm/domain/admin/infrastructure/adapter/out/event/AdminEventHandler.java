package jaeik.growfarm.domain.admin.infrastructure.adapter.out.event;

import jaeik.growfarm.domain.admin.application.port.out.ManageEmitterPort;
import jaeik.growfarm.domain.admin.application.port.out.SaveBlacklistPort;
import jaeik.growfarm.domain.admin.application.port.out.UserAuthPort;
import jaeik.growfarm.domain.user.domain.BlackList;
import jaeik.growfarm.global.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminEventHandler {

    private final SaveBlacklistPort saveBlacklistPort;
    private final ManageEmitterPort manageEmitterPort;
    private final UserAuthPort userAuthPort;

    @Transactional
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        BlackList blackList = BlackList.createBlackList(event.getSocialId(), event.getProvider());
        saveBlacklistPort.save(blackList);
        manageEmitterPort.deleteAllEmitterByUserId(event.getUserId());
        userAuthPort.withdraw(event.getUserId());
    }
}
