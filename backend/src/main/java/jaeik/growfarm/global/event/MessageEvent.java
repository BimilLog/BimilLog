package jaeik.growfarm.global.event;

import jaeik.growfarm.entity.user.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>메시지 작성 이벤트</h2>
 * <p>
 * 다른 사용자가 롤링페이퍼에 메시지를 남겼을 때 발생하는 이벤트
 * SSE와 FCM 알림을 트리거한다
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class MessageEvent {

    /**
     * 롤링페이퍼 주인 ID (알림을 받을 사용자)
     */
    private final Long paperOwnerId;

    /**
     * 닉네임
     */
    private final String userName;

    /**
     * 농장 주인 엔티티 (FCM 토큰 조회용)
     */
    private final Users paperOwner;
}