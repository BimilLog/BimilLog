package jaeik.bimillog.domain.friend.event;

import jaeik.bimillog.domain.friend.listener.FriendshipRedisListener;

/**
 * <h2>친구 관계 삭제 이벤트</h2>
 * <p>친구 삭제 시 발생하는 이벤트</p>
 * <p>Redis 친구 관계 테이블에서 양방향으로 친구를 삭제합니다</p>
 *
 * @param memberId 친구 관계의 한 쪽 사용자 ID
 * @param friendId 친구 관계의 다른 쪽 사용자 ID
 * @author Jaeik
 * @version 2.0.0
 * {@link FriendshipRedisListener} Redis 친구 관계 동기화
 */
public record FriendshipDeletedEvent(
        Long memberId,
        Long friendId
) {
    public FriendshipDeletedEvent {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId는 null일 수 없습니다.");
        }
        if (friendId == null) {
            throw new IllegalArgumentException("friendId는 null일 수 없습니다.");
        }
    }
}
