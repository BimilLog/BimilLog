package jaeik.bimillog.domain.paper.event;

/**
 * <h2>롤링페이퍼 도메인 이벤트</h2>
 * <p>롤링페이퍼 도메인에서 발생하는 모든 이벤트를 정의합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public interface PaperEvent {

    /**
     * <h3>롤링페이퍼 조회 이벤트</h3>
     * <p>롤링페이퍼가 조회되었을 때 발생하는 비동기 이벤트</p>
     * <p>RealtimePaperPopularScoreListener에서 수신하여 실시간 인기 점수를 2점 증가시킵니다.</p>
     *
     * @param memberId 롤링페이퍼 소유자 ID (점수를 증가시킬 대상)
     */
    record PaperViewedEvent(Long memberId) implements PaperEvent {}

    /**
     * <h3>롤링페이퍼 메시지 삭제 이벤트</h3>
     * <p>롤링페이퍼 메시지가 삭제되었을 때 발생하는 비동기 이벤트</p>
     * <p>RealtimePaperPopularScoreListener에서 수신하여 실시간 인기 점수를 5점 감소시킵니다.</p>
     *
     * @param paperOwnerId 롤링페이퍼 소유자 ID (점수를 감소시킬 대상)
     */
    record MessageDeletedEvent(Long paperOwnerId) implements PaperEvent {}

    /**
     * <h3>롤링페이퍼 메시지 작성 이벤트</h3>
     * <p>다른 사용자가 롤링페이퍼에 메시지를 남겼을 때 발생하는 이벤트</p>
     * <p>NotificationSaveListener에서 수신하여 SSE와 FCM 알림을 트리거합니다.</p>
     * <p>RealtimePaperPopularScoreListener에서 수신하여 실시간 인기 점수를 5점 증가시킵니다.</p>
     *
     * @param paperOwnerId 롤링페이퍼 주인 ID (알림을 받을 사용자)
     * @param memberName   메시지 작성자 닉네임
     */
    record RollingPaperEvent(Long paperOwnerId, String memberName) implements PaperEvent {}
}
