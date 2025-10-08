package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.admin.event.MemberBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.AuthTokenUseCase;
import jaeik.bimillog.domain.auth.application.port.in.KakaoTokenUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 차단 이벤트 리스너</h2>
 * <p>관리자에 의한 사용자 차단 시 발생하는 {@link MemberBannedEvent}를 처리합니다.</p>
 * <p>SSE 연결 종료, 소셜 계정 강제 로그아웃, FCM/JWT 토큰 삭제, 카카오 토큰 삭제를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberBannedListener {

    private final SocialLogoutUseCase socialLogoutUseCase;
    private final FcmUseCase fcmUseCase;
    private final AuthTokenUseCase authTokenUseCase;
    private final SseUseCase sseUseCase;
    private final KakaoTokenUseCase kakaoTokenUseCase;

    /**
     * <h3>사용자 차단 이벤트 처리</h3>
     * <p>관리자가 사용자를 차단할 때 발생하는 이벤트를 처리합니다.</p>
     * <p>FCM 토큰 삭제, 소셜 플랫폼 강제 로그아웃, JWT 토큰 무효화, 카카오 토큰 삭제를 수행합니다.</p>
     *
     * @param memberBannedEvent 사용자 차단 이벤트 (memberId, socialId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void memberBanned(MemberBannedEvent memberBannedEvent) {
        Long memberId = memberBannedEvent.memberId();
        String socialId = memberBannedEvent.socialId();
        SocialProvider provider = memberBannedEvent.provider();

        sseUseCase.deleteEmitters(memberId, null);
        socialLogoutUseCase.forceLogout(memberId, provider, socialId); // 구현 필요
        fcmUseCase.deleteFcmTokens(memberId, null);
        authTokenUseCase.deleteTokens(memberId, null);
        kakaoTokenUseCase.deleteByMemberId(memberId);
    }
}
