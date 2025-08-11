
package jaeik.growfarm.domain.auth.application.handler;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.global.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBannedEventHandler {

    private final SocialLoginPort socialLoginPort;

    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        socialLoginPort.unlink(event.getProvider(), event.getSocialId());
    }
}
