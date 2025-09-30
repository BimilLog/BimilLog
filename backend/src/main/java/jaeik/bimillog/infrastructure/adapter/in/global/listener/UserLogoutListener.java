package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.auth.application.port.in.KakaoTokenCommandUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.in.TokenUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLogoutListener {

    private final SocialLogoutUseCase socialLogoutUseCase;
    private final SseUseCase sseUseCase;
    private final FcmUseCase fcmUseCase;
    private final TokenUseCase tokenUseCase;
    private final KakaoTokenCommandUseCase kakaoTokenCommandUseCase;

    /**
     * <h3>사용자 로그아웃 이벤트 처리</h3>
     * <p>사용자가 로그아웃할 때 발생하는 이벤트를 비동기로 처리합니다.</p>
     * <p>SSE 연결 종료, 소셜 플랫폼 로그아웃, FCM 토큰 삭제, JWT 토큰 무효화, 카카오 토큰 삭제를 순차적으로 수행합니다.</p>
     *
     * @param userLoggedOutEvent 로그아웃 이벤트 (userId, tokenId, fcmTokenId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void userLogout(UserLoggedOutEvent userLoggedOutEvent) {
        Long userId = userLoggedOutEvent.userId();
        Long tokenId = userLoggedOutEvent.tokenId();
        Long fcmTokenId = userLoggedOutEvent.fcmTokenId();
        SocialProvider provider = userLoggedOutEvent.provider();

        sseUseCase.deleteEmitters(userId, tokenId);
        socialLogoutUseCase.logout(userId, provider, tokenId);
        fcmUseCase.deleteFcmTokens(userId, fcmTokenId);
        tokenUseCase.deleteTokens(userId, tokenId);
        kakaoTokenCommandUseCase.deleteByUserId(userId);
        SecurityContextHolder.clearContext();

    }
}
