package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.entity.notification.FcmToken;
import java.util.List;

public interface LoadFcmTokenPort {
    List<FcmToken> findValidFcmTokensByUserId(Long userId);
}
