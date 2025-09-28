package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.in.TokenUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
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
        CustomUserDetails userDetails = userLoggedOutEvent.userDetails();
        Long userId = userLoggedOutEvent.userDetails().getUserId();
        Long tokenId = userLoggedOutEvent.userDetails().getTokenId();

        sseUseCase.deleteEmitterByUserIdAndTokenId(userId, tokenId);
        socialLogoutUseCase.logout(userDetails);
        fcmUseCase.deleteFcmTokenByTokenId(userId, tokenId); // 구현 필요
        tokenUseCase.deleteTokens(userId, tokenId);
        SecurityContextHolder.clearContext();

    }
}
