package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;

import java.io.IOException;

/**
 * <h2>FCM 포트</h2>
 * <p>Firebase Cloud Messaging 연동을 담당하는 포트입니다.</p>
 * <p>FCM 토큰 저장, FCM 토큰 삭제, FCM 메시지 전송</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface FcmPort {
    
    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>회원탈퇴 시 해당 사용자의 모든 FCM 토큰을 데이터베이스에서 삭제합니다.</p>
     * <p>다중 기기에 등록된 모든 토큰을 일괄 제거하여 푸시 알림 전송 중단</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUserId(Long userId);

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

    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>Firebase Cloud Messaging 서비스를 통해 푸시 알림을 전송합니다.</p>
     * <p>Firebase Admin SDK로 실제 푸시 메시지 발송, 전송 실패 시 예외 발생</p>
     *
     * @param fcmMessage 전송할 FCM 메시지 정보
     * @throws IOException Firebase 서비스 통신 중 발생하는 네트워크 예외
     * @author Jaeik
     * @since 2.0.0
     */
    void sendMessageTo(FcmMessage fcmMessage) throws IOException;


}