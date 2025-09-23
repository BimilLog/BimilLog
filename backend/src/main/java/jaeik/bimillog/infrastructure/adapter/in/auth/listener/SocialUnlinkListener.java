
package jaeik.bimillog.infrastructure.adapter.in.auth.listener;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
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

    private final SocialWithdrawUseCase socialWithdrawUseCase;

    /**
     * <h3>사용자 탈퇴 이벤트 처리 - 소셜 연결 해제</h3>
     * <p>사용자가 자발적으로 탈퇴할 때 발생하는 UserWithdrawnEvent를 수신하여 소셜 로그인 연결을 해제합니다.</p>
     *
     * @param event 사용자 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handleUserWithdrawn(UserWithdrawnEvent event) {
        socialUnlink(event.userId(), event.provider(), event.socialId());
    }

    /**
     * <h3>사용자 차단 이벤트 처리 - 소셜 연결 해제</h3>
     * <p>관리자가 사용자를 강제 차단할 때 발생하는 UserBannedEvent를 수신하여 소셜 로그인 연결을 해제합니다.</p>
     *
     * @param event 사용자 차단 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handleUserBanned(UserBannedEvent event) {
        socialUnlink(event.userId(), event.provider(), event.socialId());
    }

    /**
     * <h3>소셜 연결 해제 공통 처리</h3>
     * <p>사용자 탈퇴나 차단 시 외부 소셜 서비스와의 연결을 해제하는 공통 메서드입니다.</p>
     *
     * @param userId 사용자 ID
     * @param socialProvider 소셜 제공자 (KAKAO 등)
     * @param socialId 소셜 서비스 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void socialUnlink(Long userId, SocialProvider socialProvider, String socialId) {
        try {
            log.info("소셜 연결 해제 시작: userId={}, provider={}, socialId={}",
                    userId, socialProvider, socialId);

            socialWithdrawUseCase.unlinkSocialAccount(socialProvider, socialId);

            log.info("소셜 연결 해제 완료: userId={}, provider={}, socialId={}",
                    userId, socialProvider, socialId);
        } catch (Exception e) {
            log.error("소셜 연결 해제 실패: userId={}, provider={}, socialId={}, error={}",
                    userId, socialProvider, socialId, e.getMessage(), e);
        }
    }
}
