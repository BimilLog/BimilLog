package jaeik.bimillog.domain.friend.event;

/**
 * <h2>친구 관계 생성 이벤트</h2>
 * <p>친구 관계 생성 후 Redis에 반영하기 위한 이벤트</p>
 *
 * @param memberId 회원 ID
 * @param friendId 친구 ID
 * @author Jaeik
 * @version 2.8.0
 */
public record FriendshipCreatedEvent(Long memberId, Long friendId) {}
