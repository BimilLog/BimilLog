package jaeik.growfarm.domain.auth.event;

/**
 * <h2>FCM 토큰 등록 이벤트</h2>
 * <p>사용자 로그인 또는 회원가입 시 FCM 토큰을 등록하기 위한 이벤트</p>
 *
 * @param userId   사용자 ID
 * @param fcmToken FCM 토큰 값
 * @author Jaeik
 * @version 2.0.0
 */
public record FcmTokenRegisteredEvent(Long userId, String fcmToken) {

    /**
     * <h3>FCM 토큰 등록 이벤트 생성</h3>
     *
     * @param userId   사용자 ID
     * @param fcmToken FCM 토큰
     * @return FcmTokenRegisteredEvent 인스턴스
     */
    public static FcmTokenRegisteredEvent of(Long userId, String fcmToken) {
        return new FcmTokenRegisteredEvent(userId, fcmToken);
    }
}
