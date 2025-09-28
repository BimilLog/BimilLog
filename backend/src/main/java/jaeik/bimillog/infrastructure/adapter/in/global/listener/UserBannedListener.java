package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.in.TokenUseCase;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
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

    @Async
    @EventListener
    @Transactional
    public void userBanned(UserBannedEvent userBannedEvent) {
        Long userId = userBannedEvent.userId();
        String socialId = userBannedEvent.socialId();
        SocialProvider provider = userBannedEvent.provider();

        fcmUseCase.deleteFcmTokens(userId);
        socialLogoutUseCase.forceLogout(userId, provider, socialId); // 구현 필요
        fcmUseCase.deleteFcmTokens(userId);
        tokenUseCase.deleteTokens(userId, null);
        SecurityContextHolder.clearContext();
    }
}
