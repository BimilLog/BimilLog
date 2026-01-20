package jaeik.bimillog.domain.friend.entity;

/**
 * <h2>친구 이벤트 타입</h2>
 * <p>DLQ에 저장되는 친구 관련 이벤트의 타입을 정의합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
public enum FriendEventType {
    /** 친구 추가 - Redis 친구 관계 Set에 추가 */
    FRIEND_ADD,

    /** 친구 삭제 - Redis 친구 관계 Set에서 삭제 */
    FRIEND_REMOVE,

    /** 상호작용 점수 증가 - Redis 상호작용 Hash에 점수 추가 */
    SCORE_UP
}
