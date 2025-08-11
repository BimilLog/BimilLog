package jaeik.growfarm.global.listener.handler;

import jaeik.growfarm.domain.user.domain.BlackList;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.repository.user.BlackListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserBannedEventHandler {

    private final BlackListRepository blackListRepository;

    @EventListener
    @Transactional
    public void handleUserBannedEvent(UserBannedEvent event) {
        BlackList blackList = BlackList.createBlackList(event.getSocialId(), event.getProvider());
        blackListRepository.save(blackList);
    }
}
