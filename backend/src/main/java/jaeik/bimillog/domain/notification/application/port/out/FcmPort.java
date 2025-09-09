package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;

import java.io.IOException;

/**
 * <h2>FCM 포트</h2>
 * <p>
 * 헥사고날 아키텍처에서 Firebase Cloud Messaging(FCM) 연동을 정의하는 Secondary Port입니다.
 * FCM 토큰 저장소 관리와 Firebase 서비스를 통한 푸시 알림 전송에 대한 외부 어댑터 인터페이스를 제공합니다.
 * </p>
 * <p>
 * FCM 토큰의 저장, 삭제와 실제 Firebase 서비스로의 푸시 메시지 전송을 처리합니다.
 * NotificationFcmService에서 사용되며, FcmAdapter에 의해 구현됩니다.
 * </p>
 * <p>FcmAdapter에서 Firebase SDK와 데이터베이스 연동을 통해 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface FcmPort {
    
    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>회원탈퇴 시 개인정보 보호를 위해 해당 사용자의 모든 FCM 토큰을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>다중 기기에 등록된 모든 FCM 토큰을 일괄적으로 제거하여 더 이상 푸시 알림이 전송되지 않도록 합니다.</p>
     * <p>NotificationFcmService에서 사용자 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUserId(Long userId);

    /**
     * <h3>FCM 토큰 저장</h3>
     * <p>클라이언트에서 등록한 FCM 토큰을 데이터베이스에 저장합니다.</p>
     * <p>중복된 토큰이 있는 경우 업데이트 시간을 갱신하며, 새로운 토큰인 경우 새로 생성합니다.</p>
     * <p>NotificationFcmService에서 FCM 토큰 등록 시 호출됩니다.</p>
     *
     * @param fcmToken 저장할 FCM 토큰 엔티티 (사용자 ID, 토큰 값, 등록 시간 포함)
     * @return 저장된 FCM 토큰 엔티티 (생성된 ID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    FcmToken save(FcmToken fcmToken);

    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>Firebase Cloud Messaging 서비스를 통해 특정 기기로 푸시 알림을 전송합니다.</p>
     * <p>Firebase Admin SDK를 사용하여 실제 푸시 메시지를 발송하며, 전송 실패 시 예외를 발생시킵니다.</p>
     * <p>NotificationFcmService에서 각종 알림 전송 시 호출됩니다.</p>
     *
     * @param fcmMessage 전송할 FCM 메시지 정보 (토큰, 제목, 내용, 데이터 포함)
     * @throws IOException Firebase 서비스 통신 중 발생할 수 있는 네트워크 예외
     * @author Jaeik
     * @since 2.0.0
     */
    void sendMessageTo(FcmMessage fcmMessage) throws IOException;


}