package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.domain.FcmToken;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FcmJpaAdapter implements LoadFcmPort {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public List<FcmToken> findValidFcmTokensByUserId(Long userId) {
        return fcmTokenRepository.findValidFcmTokensByUserId(userId);
    }
}
