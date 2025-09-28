package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.in.TokenUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
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

    @Async
    @EventListener
    @Transactional
    public void userLogout(UserLoggedOutEvent userLoggedOutEvent) {
        Long userId = userLoggedOutEvent.userId();
        Long tokenId = userLoggedOutEvent.tokenId();
        Long fcmTokenId = userLoggedOutEvent.fcmTokenId();
        SocialProvider provider = userLoggedOutEvent.provider();

        sseUseCase.deleteEmitterByUserIdAndTokenId(userId, tokenId);
        socialLogoutUseCase.logout(userId, provider, tokenId);
        fcmUseCase.deleteFcmTokenByTokenId(userId, fcmTokenId); // 구현 필요
        tokenUseCase.deleteTokens(userId, tokenId);
        SecurityContextHolder.clearContext();

    }
}
