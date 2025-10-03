package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.auth.application.port.in.KakaoTokenUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.in.AuthTokenUseCase;
import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 로그아웃 이벤트 리스너</h2>
 * <p>사용자 로그아웃 시 발생하는 {@link MemberLoggedOutEvent}를 비동기로 처리합니다.</p>
 * <p>SSE 연결 종료, 소셜 플랫폼 로그아웃, FCM/JWT 토큰 삭제, 카카오 토큰 삭제를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberLogoutListener {

    private final SocialLogoutUseCase socialLogoutUseCase;
    private final SseUseCase sseUseCase;
    private final FcmUseCase fcmUseCase;
    private final AuthTokenUseCase authTokenUseCase;
    private final KakaoTokenUseCase kakaoTokenUseCase;

    /**
     * <h3>사용자 로그아웃 이벤트 처리</h3>
     * <p>사용자가 로그아웃할 때 발생하는 이벤트를 비동기로 처리합니다.</p>
     * <p>SSE 연결 종료, 소셜 플랫폼 로그아웃, FCM 토큰 삭제, JWT 토큰 무효화, 카카오 토큰 삭제를 순차적으로 수행합니다.</p>
     *
     * @param memberLoggedOutEvent 로그아웃 이벤트 (memberId, authTokenId, fcmTokenId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void memberLogout(MemberLoggedOutEvent memberLoggedOutEvent) throws Exception {
        Long memberId = memberLoggedOutEvent.memberId();
        Long AuthTokenId = memberLoggedOutEvent.authTokenId();
        Long fcmTokenId = memberLoggedOutEvent.fcmTokenId();
        SocialProvider provider = memberLoggedOutEvent.provider();

        sseUseCase.deleteEmitters(memberId, AuthTokenId);
        socialLogoutUseCase.socialLogout(memberId, provider, AuthTokenId);
        fcmUseCase.deleteFcmTokens(memberId, fcmTokenId);
        authTokenUseCase.deleteTokens(memberId, AuthTokenId);
        kakaoTokenUseCase.deleteByMemberId(memberId);
        SecurityContextHolder.clearContext();

    }
}
