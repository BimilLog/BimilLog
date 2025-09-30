package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.KakaoTokenCommandUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.in.TokenUseCase;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBannedListener {

    private final SocialLogoutUseCase socialLogoutUseCase;
    private final FcmUseCase fcmUseCase;
    private final TokenUseCase tokenUseCase;
    private final SseUseCase sseUseCase;
    private final KakaoTokenCommandUseCase kakaoTokenCommandUseCase;

    /**
     * <h3>사용자 차단 이벤트 처리</h3>
     * <p>관리자가 사용자를 차단할 때 발생하는 이벤트를 처리합니다.</p>
     * <p>FCM 토큰 삭제, 소셜 플랫폼 강제 로그아웃, JWT 토큰 무효화, 카카오 토큰 삭제를 수행합니다.</p>
     *
     * @param userBannedEvent 사용자 차단 이벤트 (userId, socialId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void userBanned(UserBannedEvent userBannedEvent) {
        Long userId = userBannedEvent.userId();
        String socialId = userBannedEvent.socialId();
        SocialProvider provider = userBannedEvent.provider();

        sseUseCase.deleteEmitters(userId, null);
        socialLogoutUseCase.forceLogout(userId, provider, socialId); // 구현 필요
        fcmUseCase.deleteFcmTokens(userId, null);
        tokenUseCase.deleteTokens(userId, null);
        kakaoTokenCommandUseCase.deleteByUserId(userId);
        SecurityContextHolder.clearContext();
    }
}
