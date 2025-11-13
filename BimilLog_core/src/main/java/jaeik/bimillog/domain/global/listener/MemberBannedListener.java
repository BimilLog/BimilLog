package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.global.out.GlobalSocialTokenCommandAdapter;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 차단 이벤트 리스너</h2>
 * <p>관리자에 의한 사용자 차단 시 발생하는 {@link MemberBannedEvent}를 처리합니다.</p>
 * <p>SSE 연결 종료, 소셜 계정 강제 로그아웃, JWT 토큰 삭제, 소셜 토큰 삭제를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Service
@RequiredArgsConstructor
public class MemberBannedListener {

    private final SocialLogoutService socialLogoutService;
    private final AuthTokenService authTokenService;
    private final SseService sseService;
    private final GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    /**
     * <h3>사용자 차단 이벤트 처리</h3>
     * <p>관리자가 사용자를 차단할 때 발생하는 이벤트를 처리합니다.</p>
     * <p>소셜 플랫폼 강제 로그아웃, JWT 토큰 무효화, 소셜 토큰 삭제를 수행합니다.</p>
     * <p>v2.4: FCM 토큰은 AuthToken 삭제 시 fcmRegistrationToken 컬럼과 함께 자동 삭제됨</p>
     *
     * @param memberBannedEvent 사용자 차단 이벤트 (memberId, socialId, provider 포함)
     * @author Jaeik
     * @since 2.1.0
     */
    @Async
    @EventListener
    @Transactional
    public void memberBanned(MemberBannedEvent memberBannedEvent) {
        Long memberId = memberBannedEvent.memberId();
        String socialId = memberBannedEvent.socialId();
        SocialProvider provider = memberBannedEvent.provider();

        sseService.deleteEmitters(memberId, null);
        socialLogoutService.forceLogout(socialId, provider);
        // AuthToken 삭제 시 fcmRegistrationToken도 함께 삭제됨 (테이블 통합)
        authTokenService.deleteTokens(memberId, null);
        globalSocialTokenCommandAdapter.deleteByMemberId(memberId);
    }
}
