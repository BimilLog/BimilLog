package jaeik.bimillog.domain.friend.event;

/**
 * <h2>친구 도메인 이벤트</h2>
 * <p>친구 도메인에서 발생하는 모든 이벤트를 정의합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public interface FriendEvent {

    /**
     * <h3>친구 요청 이벤트</h3>
     * <p>친구 요청이 전송되었을 때 발생하는 이벤트</p>
     * <p>NotificationSaveListener에서 수신하여 알림을 저장하고 SSE와 FCM 알림을 비동기로 발송합니다.</p>
     * <p>FCM 푸시 알림의 title과 body는 FcmCommandService에서 작성됩니다.</p>
     *
     * @param receiveMemberId 알림을 받을 회원 ID
     * @param sseMessage      SSE 알림 메시지
     * @param senderName      FCM 알림에 사용될 친구 요청 보낸 사람 이름
     */
    record FriendRequestEvent(Long receiveMemberId, String sseMessage, String senderName) implements FriendEvent {}

    /**
     * <h3>친구 관계 생성 이벤트</h3>
     * <p>친구 관계 생성 후 Redis에 반영하기 위한 이벤트</p>
     *
     * @param memberId 회원 ID
     * @param friendId 친구 ID
     */
    record FriendshipCreatedEvent(Long memberId, Long friendId) implements FriendEvent {}

    /**
     * <h3>친구 관계 삭제 이벤트</h3>
     * <p>친구 관계 삭제 후 Redis에서 제거하기 위한 이벤트</p>
     *
     * @param memberId1 회원 ID
     * @param memberId2 친구 ID
     */
    record FriendshipDeletedEvent(Long memberId1, Long memberId2) implements FriendEvent {}
}
