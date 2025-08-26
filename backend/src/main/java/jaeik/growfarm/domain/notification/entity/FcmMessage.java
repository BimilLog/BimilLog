package jaeik.growfarm.domain.notification.entity;

import lombok.Builder;

/**
 * <h3>FCM 메시지 값 객체</h3>
 * <p>
 * Firebase Cloud Messaging 전송을 위한 도메인 순수 값 객체
 * FcmSendDTO의 도메인 전용 대체
 * </p>
 *
 * @param token FCM 토큰
 * @param title 알림 제목
 * @param body 알림 내용
 * @author Jaeik
 * @since 2.0.0
 */
public record FcmMessage(
        String token,
        String title,
        String body
) {

    @Builder
    public FcmMessage {
    }

    /**
     * <h3>FCM 메시지 생성</h3>
     * <p>토큰과 제목, 내용으로 FCM 메시지를 생성합니다.</p>
     *
     * @param token FCM 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @return FcmMessage 값 객체
     */
    public static FcmMessage of(String token, String title, String body) {
        return FcmMessage.builder()
                .token(token)
                .title(title)
                .body(body)
                .build();
    }
}