package jaeik.growfarm.domain.notification.application.port.in;

/**
 * <h2>FCM 토큰 관리 유스케이스</h2>
 * <p>Firebase Cloud Messaging(FCM) 토큰 등록 및 삭제 관련 비즈니스 로직을 정의하는 Primary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationFcmUseCase {

    /**
     * <h3>FCM 토큰 등록 처리</h3>
     * <p>사용자 로그인 또는 회원가입 시 FCM 토큰을 등록합니다.</p>
     *
     * @param userId   사용자 ID
     * @param fcmToken FCM 토큰 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    void registerFcmToken(Long userId, String fcmToken);

    /**
     * <h3>FCM 토큰 삭제 처리</h3>
     * <p>사용자 로그아웃 또는 탈퇴 시 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteFcmTokens(Long userId);
}
