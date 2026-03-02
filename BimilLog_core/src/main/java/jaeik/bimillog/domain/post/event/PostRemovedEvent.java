package jaeik.bimillog.domain.post.event;

/**
 * <h2>글 삭제 캐시 이벤트</h2>
 * <p>게시글 삭제 후 모든 캐시(ZSet + JSON LIST)를 정리하기 위한 이벤트</p>
 *
 * @param postId 삭제된 게시글 ID
 * @author Jaeik
 * @version 2.8.0
 */
public record PostRemovedEvent(Long postId) {}
