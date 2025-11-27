package jaeik.bimillog.domain.friend.event;

import jaeik.bimillog.domain.friend.listener.FriendshipRedisListener;

/**
 * <h2>친구 관계 생성 이벤트</h2>
 * <p>친구 요청 수락 시 발생하는 이벤트</p>
 * <p>Redis 친구 관계 테이블에 양방향으로 친구를 추가합니다</p>
 *
 * @param memberId 친구 관계를 생성한 사용자 ID
 * @param friendId 친구가 된 사용자 ID
 * @author Jaeik
 * @version 2.0.0
 * {@link FriendshipRedisListener} Redis 친구 관계 동기화
 */
public record FriendshipCreatedEvent(
        Long memberId,
        Long friendId
) {
    public FriendshipCreatedEvent {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId는 null일 수 없습니다.");
        }
        if (friendId == null) {
            throw new IllegalArgumentException("friendId는 null일 수 없습니다.");
        }
    }
}
