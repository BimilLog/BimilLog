
package jaeik.bimillog.infrastructure.adapter.auth.in.listener;

import jaeik.bimillog.domain.auth.application.port.in.SocialUnlinkUseCase;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>사용자 차단 이벤트 리스너</h2>
 * <p>사용자가 차단되었을 때 소셜 로그인을 해제하는 이벤트 리스너</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUnlinkListener {

    private final SocialUnlinkUseCase socialUnlinkUseCase;

    /**
     * <h3>사용자 차단 이벤트 핸들러</h3>
     * <p>사용자가 차단되었을 때 소셜 로그인을 해제합니다.</p>
     *
     * @param event 사용자 차단 이벤트
     * @since 2.0.0
     * @author Jaeik
     */
    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        try {
            log.info("사용자 차단 이벤트 수신 - 소셜 연결 해제 시작: userId={}, provider={}, socialId={}", 
                    event.getUserId(), event.getProvider(), event.getSocialId());
            
            socialUnlinkUseCase.unlinkSocialAccount(event.getProvider(), event.getSocialId());
            
            log.info("소셜 연결 해제 완료: userId={}, provider={}, socialId={}", 
                    event.getUserId(), event.getProvider(), event.getSocialId());
        } catch (Exception e) {
            log.error("소셜 연결 해제 실패: userId={}, provider={}, socialId={}, error={}", 
                    event.getUserId(), event.getProvider(), event.getSocialId(), e.getMessage(), e);
        }
    }
}
