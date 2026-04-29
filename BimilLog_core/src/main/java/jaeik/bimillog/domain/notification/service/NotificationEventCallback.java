package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.notification.event.AlarmSendEvent;

/**
 * <h2>알림 이벤트 생성 콜백</h2>
 * <p>알림 저장 템플릿에서 타입별 AlarmSendEvent 생성을 위임받는 콜백</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@FunctionalInterface
public interface NotificationEventCallback {
    AlarmSendEvent createEvent(Member member);
}
