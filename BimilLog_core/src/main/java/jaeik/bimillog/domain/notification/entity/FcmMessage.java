package jaeik.bimillog.domain.notification.entity;

/**
 * <h2>FCM 메시지 값 객체</h2>
 * <p>Firebase Cloud Messaging 전송을 위한 도메인 순수 값 객체입니다.</p>
 * <p>FCM 푸시 알림 전송에 필요한 토큰, 제목, 내용 보관</p>
 *
 * @param token FCM 토큰
 * @param title 알림 제목
 * @param body 알림 내용
 * @author Jaeik
 * @version 2.0.0
 */
public record FcmMessage(
        String token,
        String title,
        String body
) {


    /**
     * <h3>FCM 메시지 생성</h3>
     * <p>토큰과 제목, 내용으로 FCM 메시지를 생성합니다.</p>
     *
     * @param token FCM 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @return FcmMessage 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static FcmMessage of(String token, String title, String body) {
        return new FcmMessage(token, title, body);
    }
}