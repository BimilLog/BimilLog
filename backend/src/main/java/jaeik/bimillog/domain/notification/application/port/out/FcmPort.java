package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;

import java.io.IOException;

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


}