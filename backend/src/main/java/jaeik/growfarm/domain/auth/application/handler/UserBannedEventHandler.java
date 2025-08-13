
package jaeik.growfarm.domain.auth.application.handler;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.global.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>사용자 차단 이벤트 핸들러</h2>
 * <p>사용자가 차단되었을 때 소셜 로그인을 해제하는 이벤트 핸들러</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserBannedEventHandler {

    private final SocialLoginPort socialLoginPort;

    /**
     * <h3>사용자 차단 이벤트 처리</h3>
     * <p>사용자가 차단되었을 때 소셜 로그인을 해제합니다.</p>
     *
     * @param event 사용자 차단 이벤트
     * @since 2.0.0
     * @author Jaeik
     */
    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        socialLoginPort.unlink(event.getProvider(), event.getSocialId());
    }
}
