package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;

import java.io.IOException;
import java.util.List;

/**
 * <h2>FCM 포트</h2>
 * <p>Firebase Cloud Messaging(FCM) 토큰 관리 및 메시지 전송을 처리하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface FcmPort {
    
    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUserId(Long userId);

    /**
     * <h3>FCM 토큰 저장</h3>
     * <p>FCM 토큰 엔티티를 저장합니다.</p>
     *
     * @param fcmToken 저장할 FCM 토큰 엔티티
     * @return 저장된 FCM 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    FcmToken save(FcmToken fcmToken);

    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>주어진 FCM 전송 DTO를 사용하여 메시지를 특정 기기로 전송합니다.</p>
     *
     * @param fcmMessage 전송할 FCM 메시지 정보
     * @throws IOException 메시지 전송 중 발생할 수 있는 IO 예외
     * @author Jaeik
     * @since 2.0.0
     */
    void sendMessageTo(FcmMessage fcmMessage) throws IOException;

    /**
     * <h3>알림 타입별 유효한 FCM 토큰 조회</h3>
     * <p>특정 알림 타입이 활성화된 사용자의 FCM 토큰 목록을 조회합니다.</p>
     *
     * @param userId 조회할 사용자의 ID
     * @param notificationType 알림 타입 (PAPER, COMMENT, POST_FEATURED)
     * @return FCM 토큰 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<FcmToken> findValidFcmTokensByNotificationType(Long userId, NotificationType notificationType);

    /**
     * <h3>알림 전송</h3>
     * <p>
     * 사용자에게 알림을 전송합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param type   알림 유형
     * @param message 알림 메시지
     * @param url    알림 URL
     * @author Jaeik
     * @since 2.0.0
     */
    void send(Long userId, NotificationType type, String message, String url);
}