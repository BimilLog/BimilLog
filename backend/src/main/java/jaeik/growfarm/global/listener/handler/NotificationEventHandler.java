package jaeik.growfarm.global.listener.handler;

import org.springframework.context.ApplicationEvent;

/**
 * <h2>알림 이벤트 핸들러 인터페이스</h2>
 * <p>
 *     모든 알림 관련 이벤트 핸들러가 구현해야 하는 공통 인터페이스입니다.
 *     특정 타입의 이벤트를 지원하는지 확인하고, 해당 이벤트를 처리하는 메서드를 정의합니다.
 * </p>
 *
 * @param <E> 처리할 이벤트의 타입, ApplicationEvent를 상속해야 합니다.
 * @author jaeik
 * @version 1.0.0
 */
public interface NotificationEventHandler<E extends ApplicationEvent> {

    /**
     * <h3>이벤트 지원 여부 확인</h3>
     * <p>
     *     이 핸들러가 주어진 이벤트를 처리할 수 있는지 확인합니다.
     * </p>
     * @param event 확인할 이벤트 객체
     * @return 지원하는 경우 true, 그렇지 않으면 false
     */
    boolean supports(ApplicationEvent event);

    /**
     * <h3>이벤트 처리</h3>
     * <p>
     *     지원하는 타입의 이벤트를 받아 실제 알림 처리 로직을 수행합니다.
     * </p>
     * @param event 처리할 이벤트 객체
     */
    void handle(E event);
}
