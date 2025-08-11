package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.domain.notification.domain.FcmToken;
import java.util.List;

public interface LoadFcmTokenPort {
    List<FcmToken> findValidFcmTokensByUserId(Long userId);
}
