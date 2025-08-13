package jaeik.growfarm.global.event;

/**
 * <h2>게시물 삭제 이벤트</h2>
 * <p>게시물이 삭제되었을 때 발생하는 이벤트</p>
 *
 * @param postId 삭제된 게시물의 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record PostDeletedEvent(Long postId) {
}
