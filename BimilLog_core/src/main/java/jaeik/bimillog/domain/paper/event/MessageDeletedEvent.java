package jaeik.bimillog.domain.paper.event;

import jaeik.bimillog.domain.paper.in.listener.RealtimePaperPopularScoreListener;

/**
 * <h2>롤링페이퍼 메시지 삭제 이벤트</h2>
 * <p>롤링페이퍼 메시지가 삭제되었을 때 발생하는 비동기 이벤트</p>
 * <p>PaperCommandService에서 메시지 삭제 완료 후 발생합니다.</p>
 * <p>RealtimePaperPopularScoreListener에서 수신하여 비동기로 롤링페이퍼의 실시간 인기 점수를 5점 감소시킵니다.</p>
 *
 * @param paperOwnerId 롤링페이퍼 소유자 ID (점수를 감소시킬 대상)
 * @author Jaeik
 * @version 2.0.0
 * {@link RealtimePaperPopularScoreListener} 실시간 인기 점수 비동기 감소
 */
public record MessageDeletedEvent(Long paperOwnerId) {
    public MessageDeletedEvent {
        if (paperOwnerId == null) {
            throw new IllegalArgumentException("롤링페이퍼 소유자 ID는 null일 수 없습니다.");
        }
    }
}
