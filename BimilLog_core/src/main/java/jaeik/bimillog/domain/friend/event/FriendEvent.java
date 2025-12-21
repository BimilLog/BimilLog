package jaeik.bimillog.domain.friend.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>친구 요청 이벤트</h2>
 * <p>친구 요청이 전송되었을 때 발생하는 이벤트</p>
 * <p>NotificationSaveListener에서 수신하여 알림을 저장하고 SSE와 FCM 알림을 비동기로 발송합니다.</p>
 * <p>FCM 푸시 알림의 title과 body는 FcmCommandService에서 작성됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public class FriendEvent {
    private Long receiveMemberId;
    private String sseMessage;
    private String senderName; // FCM 알림에 사용될 친구 요청 보낸 사람 이름
}
