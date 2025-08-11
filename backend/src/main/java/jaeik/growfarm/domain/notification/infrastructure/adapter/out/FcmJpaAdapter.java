package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.LoadFcmTokenPort;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FcmJpaAdapter implements LoadFcmTokenPort {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public List<FcmToken> findValidFcmTokensByUserId(Long userId) {
        return fcmTokenRepository.findValidFcmTokensByUserId(userId);
    }
}
