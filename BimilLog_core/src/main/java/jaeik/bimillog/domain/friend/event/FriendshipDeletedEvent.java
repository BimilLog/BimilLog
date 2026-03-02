package jaeik.bimillog.domain.friend.event;

/**
 * <h2>친구 관계 삭제 이벤트</h2>
 * <p>친구 관계 삭제 후 Redis에서 제거하기 위한 이벤트</p>
 *
 * @param memberId1 회원 ID
 * @param memberId2 친구 ID
 * @author Jaeik
 * @version 2.8.0
 */
public record FriendshipDeletedEvent(Long memberId1, Long memberId2) {}
