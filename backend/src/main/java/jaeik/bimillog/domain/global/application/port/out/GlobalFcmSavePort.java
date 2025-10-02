package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.notification.entity.FcmToken;

public interface GlobalFcmSavePort {

    /**
     * <h3>FCM 토큰 저장</h3>
     * <p>FCM 토큰 엔티티를 데이터베이스에 저장합니다.</p>
     * <p>중복 토큰 경우 업데이트, 새 토큰 경우 생성</p>
     *
     * @param fcmToken 저장할 FCM 토큰 엔티티
     * @return 저장된 FCM 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    FcmToken save(FcmToken fcmToken);
}
