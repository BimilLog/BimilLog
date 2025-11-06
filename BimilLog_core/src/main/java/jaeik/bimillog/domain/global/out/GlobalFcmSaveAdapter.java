package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.global.application.port.out.GlobalFcmSavePort;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.out.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>전역 FCM 토큰 저장 어댑터</h2>
 * <p>{@link GlobalFcmSavePort}를 구현하여 FCM 토큰을 JpaRepository에 저장합니다.</p>
 * <p>여러 도메인에서 FCM 토큰 저장 로직을 재사용하기 위한 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
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
