package jaeik.growfarm.global.event;

import jaeik.growfarm.entity.user.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>농작물 심기 이벤트</h2>
 * <p>
 * 다른 사용자가 농장에 농작물을 심었을 때 발생하는 이벤트
 * SSE와 FCM 알림을 트리거한다
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class FarmPlantEvent {

    /**
     * 농장 주인 ID (알림을 받을 사용자)
     */
    private final Long farmOwnerId;

    /**
     * 농장 이름
     */
    private final String farmName;

    /**
     * 농장 주인 엔티티 (FCM 토큰 조회용)
     */
    private final Users farmOwner;
}