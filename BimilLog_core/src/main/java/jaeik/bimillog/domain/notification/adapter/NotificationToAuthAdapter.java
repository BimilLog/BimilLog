package jaeik.bimillog.domain.notification.adapter;

import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationToAuthAdapter {
    private final AuthTokenService authTokenService;

    public List<String> fcmEligibleFcmTokens(Long memberId, NotificationType type) {
        return authTokenService.fcmEligibleFcmTokens(memberId, type);
    }
}
