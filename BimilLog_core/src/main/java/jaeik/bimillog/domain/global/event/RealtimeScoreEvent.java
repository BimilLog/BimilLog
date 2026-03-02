package jaeik.bimillog.domain.global.event;

/**
 * <h2>실시간 인기글 점수를 변동하는 이벤트 인터페이스</h2>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public interface RealtimeScoreEvent {
    Long postId();
    double realtimeScore();
}
