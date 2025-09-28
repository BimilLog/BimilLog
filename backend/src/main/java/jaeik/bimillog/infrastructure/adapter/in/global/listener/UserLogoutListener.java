package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.auth.application.port.in.SocialLogoutUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLogoutListener {

    private final SocialLogoutUseCase socialLogoutUseCase;
    private final NotificationSseUseCase notificationSseUseCase;
    private final NotificationFcmUseCase notificationFcmUseCase;

    @Async
    @EventListener
    @Transactional
    public void userLogout(UserLoggedOutEvent userLoggedOutEvent) {
        CustomUserDetails userDetails = userLoggedOutEvent.userDetails();
        Long userId = userLoggedOutEvent.userDetails().getUserId();
        Long tokenId = userLoggedOutEvent.userDetails().getTokenId();

        notificationSseUseCase.deleteEmitterByUserIdAndTokenId(userId, tokenId);
        socialLogoutUseCase.logout(userDetails);
        notificationFcmUseCase.deleteFcmTokenByTokenId(userId, tokenId);
    }
}
