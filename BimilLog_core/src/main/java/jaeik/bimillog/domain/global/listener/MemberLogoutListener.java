package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialLogoutService;
import jaeik.bimillog.domain.global.out.GlobalSocialTokenCommandAdapter;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 로그아웃 이벤트 리스너</h2>
 * <p>사용자 로그아웃 시 발생하는 {@link MemberLoggedOutEvent}를 비동기로 처리합니다.</p>
 * <p>SSE 연결 종료, 소셜 플랫폼 로그아웃, FCM/JWT 토큰 삭제, 소셜 토큰 삭제를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLogoutListener {

    private final SocialLogoutService socialLogoutService;
    private final SseService sseService;
    private final AuthTokenService authTokenService;
    private final GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    /**
     * <h3>사용자 로그아웃 이벤트 처리</h3>
     * <p>사용자가 로그아웃할 때 발생하는 이벤트를 비동기로 처리합니다.</p>
     * <p>SSE 연결 종료, 소셜 플랫폼 로그아웃, AuthToken 삭제(FCM 토큰 포함), 소셜 토큰 삭제를 순차적으로 수행합니다.</p>
     * <p>주의: AuthToken 삭제 시 기기별 FCM 토큰도 함께 삭제됩니다 (CASCADE)</p>
     *
     * @param memberLoggedOutEvent 로그아웃 이벤트 (memberId, authTokenId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void memberLogout(MemberLoggedOutEvent memberLoggedOutEvent) throws Exception {
        Long memberId = memberLoggedOutEvent.memberId();
        Long AuthTokenId = memberLoggedOutEvent.authTokenId();
        SocialProvider provider = memberLoggedOutEvent.provider();

        sseService.deleteEmitters(memberId, AuthTokenId);
        try {
            socialLogoutService.socialLogout(memberId, provider);
        } catch (Exception ex) {
            log.warn("소셜 로그아웃 실패 - provider: {}, memberId: {}. 이후 정리 작업은 계속 진행합니다.", provider, memberId, ex);
        }

        authTokenService.deleteTokens(memberId, AuthTokenId);
        globalSocialTokenCommandAdapter.deleteByMemberId(memberId);
        SecurityContextHolder.clearContext();

    }
}
