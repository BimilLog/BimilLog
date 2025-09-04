package jaeik.bimillog.domain.post.event;

/**
 * <h2>게시글 조회 이벤트</h2>
 * <p>게시글이 조회되었을 때 발생하는 이벤트</p>
 * <p>조회수 증가 로직을 비동기로 처리하기 위해 사용</p>
 * <p>중복 조회 검증은 Controller에서 처리되며, 이벤트는 단순히 조회수 증가만 담당합니다.</p>
 *
 * @param postId 조회된 게시글 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record PostViewedEvent(Long postId) {
    public PostViewedEvent {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 ID는 null일 수 없습니다.");
        }
    }
}