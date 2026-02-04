package jaeik.bimillog.domain.post.entity;

/**
 * <h2>Post Read Model 이벤트 타입</h2>
 * <p>PostReadModel 동기화에 사용되는 이벤트 타입입니다.</p>
 * <p>POST_DELETED는 DB CASCADE로 자동 삭제되므로 불필요합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
public enum PostReadModelEventType {
    POST_CREATED,
    POST_UPDATED,
    LIKE_INCREMENT,
    LIKE_DECREMENT,
    COMMENT_INCREMENT,
    COMMENT_DECREMENT,
    VIEW_INCREMENT
}
