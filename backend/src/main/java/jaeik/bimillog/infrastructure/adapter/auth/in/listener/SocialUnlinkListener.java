
package jaeik.bimillog.infrastructure.adapter.auth.in.listener;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.SocialUnlinkUseCase;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>소셜 연결해제 리스너</h2>
 * <p>사용자가 탈퇴하거나 강제차단 되었을 때 소셜 로그인을 해제하는 이벤트 리스너</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUnlinkListener {

    private final SocialUnlinkUseCase socialUnlinkUseCase;

    @Async
    @EventListener
    public void handleUserWithdrawn(UserWithdrawnEvent event) {
        socialUnlink(event.userId(), event.provider(), event.socialId());
    }

    @Async
    @EventListener
    public void handleUserBanned(UserBannedEvent event) {
        socialUnlink(event.userId(), event.provider(), event.socialId());
    }

    private void socialUnlink(Long userId, SocialProvider socialProvider, String socialId) {
        try {
            log.info("소셜 연결 해제 시작: userId={}, provider={}, socialId={}",
                    userId, socialProvider, socialId);

            socialUnlinkUseCase.unlinkSocialAccount(socialProvider, socialId);

            log.info("소셜 연결 해제 완료: userId={}, provider={}, socialId={}",
                    userId, socialProvider, socialId);
        } catch (Exception e) {
            log.error("소셜 연결 해제 실패: userId={}, provider={}, socialId={}, error={}",
                    userId, socialProvider, socialId, e.getMessage(), e);
        }
    }
}
