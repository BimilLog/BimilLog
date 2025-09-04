package jaeik.bimillog.domain.paper.event;

/**
 * <h2>메시지 작성 이벤트</h2>
 * <p>다른 사용자가 롤링페이퍼에 메시지를 남겼을 때 발생하는 이벤트</p>
 * <p>SSE와 FCM 알림을 트리거합니다</p>
 *
 * @param paperOwnerId 롤링페이퍼 주인 ID (알림을 받을 사용자)
 * @param userName 닉네임
 * @author Jaeik
 * @version 2.0.0
 */
public record RollingPaperEvent(
        Long paperOwnerId,
        String userName
) {
    public RollingPaperEvent {
        if (paperOwnerId == null) {
            throw new IllegalArgumentException("롤링페이퍼 주인 ID는 null일 수 없습니다.");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("사용자 이름은 null이거나 비어있을 수 없습니다.");
        }
    }
}