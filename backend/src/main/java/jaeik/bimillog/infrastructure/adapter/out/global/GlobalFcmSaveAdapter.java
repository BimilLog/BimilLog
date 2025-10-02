package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.global.application.port.out.GlobalFcmSavePort;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.infrastructure.adapter.out.notification.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalFcmSaveAdapter implements GlobalFcmSavePort {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * <h3>FCM 토큰 저장</h3>
     * <p>FCM 토큰 엔티티를 저장합니다.</p>
     *
     * @param fcmToken 저장할 FCM 토큰 엔티티
     * @return 저장된 FCM 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public FcmToken save(FcmToken fcmToken) {
        return fcmTokenRepository.save(fcmToken);
    }
}
