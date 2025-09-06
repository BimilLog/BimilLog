package jaeik.bimillog.domain.notification.entity;

/**
 * <h3>SSE 메시지 값 객체</h3>
 * <p>
 * Server-Sent Events 전송을 위한 도메인 순수 값 객체
 * </p>
 *
 * @param userId 수신자 사용자 ID
 * @param type 알림 유형
 * @param message 알림 메시지
 * @param url 알림 클릭 시 이동할 URL
 * @author Jaeik
 * @since 2.0.0
 */
public record SseMessage(
        Long userId,
        NotificationType type,
        String message,
        String url
) {


    /**
     * <h3>SSE 메시지 생성</h3>
     * <p>사용자 ID, 알림 유형, 메시지, URL로 SSE 메시지를 생성합니다.</p>
     *
     * @param userId 수신자 사용자 ID
     * @param type 알림 유형
     * @param message 알림 메시지
     * @param url 알림 URL
     * @return SseMessage 값 객체
     */
    public static SseMessage of(Long userId, NotificationType type, String message, String url) {
        return new SseMessage(userId, type, message, url);
    }

    /**
     * <h3>JSON 형식 변환</h3>
     * <p>SSE 전송을 위한 JSON 문자열로 변환합니다.</p>
     *
     * @return JSON 형식 문자열
     */
    public String toJsonData() {
        return String.format("{\"message\": \"%s\", \"url\": \"%s\"}", message, url);
    }
}