package jaeik.bimillog.domain.paper.event;

import jaeik.bimillog.in.paper.listener.RealtimePaperPopularScoreListener;

/**
 * <h2>롤링페이퍼 조회 이벤트</h2>
 * <p>롤링페이퍼가 조회되었을 때 발생하는 비동기 이벤트</p>
 * <p>PaperQueryController에서 롤링페이퍼 방문 요청 시 발생합니다.</p>
 * <p>RealtimePaperPopularScoreListener에서 수신하여 비동기로 롤링페이퍼의 실시간 인기 점수를 2점 증가시킵니다.</p>
 *
 * @param memberId 롤링페이퍼 소유자 ID (점수를 증가시킬 대상)
 * @author Jaeik
 * @version 2.0.0
 * {@link RealtimePaperPopularScoreListener} 실시간 인기 점수 비동기 증가
 */
public record PaperViewedEvent(Long memberId) {
    public PaperViewedEvent {
        if (memberId == null) {
            throw new IllegalArgumentException("롤링페이퍼 소유자 ID는 null일 수 없습니다.");
        }
    }
}
