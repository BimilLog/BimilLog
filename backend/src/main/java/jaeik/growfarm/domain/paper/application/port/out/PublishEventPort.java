package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.global.event.MessageEvent;

/**
 * <h2>이벤트 발행 포트</h2>
 * <p>
 * Secondary Port: 도메인 이벤트 발행을 위한 포트
 * 기존 ApplicationEventPublisher의 기능을 추상화
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
public interface PublishEventPort {

    /**
     * <h3>메시지 이벤트 발행</h3>
     * <p>
     * 기존 ApplicationEventPublisher.publishEvent() 메서드와 동일한 기능
     * 메시지 작성 시 알림 시스템과의 연동을 위한 이벤트 발행
     * </p>
     *
     * @param event 발행할 메시지 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    void publishMessageEvent(MessageEvent event);
}