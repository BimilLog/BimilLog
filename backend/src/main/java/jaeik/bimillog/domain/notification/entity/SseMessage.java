package jaeik.bimillog.domain.notification.entity;

/**
 * <h2>SSE 메시지 값 객체</h2>
 * <p>
 * Server-Sent Events 전송을 위한 도메인 순수 값 객체
 * </p>
 * <p>롤링페이퍼 메시지 등록, 댓글 작성, 관리자 공지 등의 이벤트 발생 시 NotificationService에서 요청되어 실시간 브라우저 알림을 위한 SSE 메시지를 생성하는 클래스</p>
 *
 * @param memberId 수신자 회원 ID
 * @param type 알림 유형
 * @param message 알림 메시지
 * @param url 알림 클릭 시 이동할 URL
 * @author Jaeik
 * @version 2.0.0
 */
public record SseMessage(
        Long memberId,
        NotificationType type,
        String message,
        String url
) {


    /**
     * <h3>SSE 메시지 생성</h3>
     * <p>회원 ID, 알림 유형, 메시지, URL로 SSE 메시지를 생성합니다.</p>
     * <p>롤링페이퍼 메시지 등록, 댓글 작성, 관리자 공지 등의 이벤트 발생 시 NotificationService에서 실시간 브라우저 알림 전송을 위해 호출되는 메서드</p>
     *
     * @param memberId 수신자 회원 ID
     * @param type 알림 유형
     * @param message 알림 메시지
     * @param url 알림 URL
     * @return SseMessage 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static SseMessage of(Long memberId, NotificationType type, String message, String url) {
        return new SseMessage(memberId, type, message, url);
    }

    /**
     * <h3>JSON 형식 변환</h3>
     * <p>SSE 전송을 위한 JSON 문자열로 변환합니다.</p>
     * <p>SSE 연결된 클라이언트에게 실시간 알림을 전송할 때 NotificationSseAdapter에서 호출되어 브라우저에서 파싱 가능한 JSON 형태로 변환하는 메서드</p>
     *
     * @return JSON 형식 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    public String toJsonData() {
        return String.format("{\"message\": \"%s\", \"url\": \"%s\"}", message, url);
    }
}