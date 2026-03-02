package jaeik.bimillog.domain.global.event;

/**
 * <h2>JSON LIST 카운터 증감 이벤트 인터페이스</h2>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public interface CacheCountEvent {
    Long postId();
    String counterField();
    int counterDelta();
}
