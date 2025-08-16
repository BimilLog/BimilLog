package jaeik.growfarm.domain.post.event;

/**
 * <h2>게시물 공지 설정 이벤트</h2>
 * <p>게시물이 공지로 설정되었을 때 발생하는 이벤트</p>
 *
 * @param postId 공지로 설정된 게시물의 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record PostSetAsNoticeEvent(Long postId) {
}
