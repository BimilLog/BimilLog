package jaeik.growfarm.domain.paper.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.paper.application.port.out.PublishEventPort;
import jaeik.growfarm.global.event.MessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// TODO : 헥사고날 아키텍처에서는 이벤트 발행은 서비스 레이어에서 일어나야함 수정 필요.
/**
 * <h2>이벤트 발행 어댑터</h2>
 * <p>
 * Secondary Port: 도메인 이벤트 발행을 위한 어댑터
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class EventPublisherAdapter implements PublishEventPort {

    private final ApplicationEventPublisher eventPublisher;

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
    @Override
    public void publishMessageEvent(MessageEvent event) {
        eventPublisher.publishEvent(event);
    }
}