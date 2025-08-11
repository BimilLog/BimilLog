package jaeik.growfarm.domain.paper.infrastructure.adapter.out;

import jaeik.growfarm.domain.paper.application.port.out.PublishEventPort;
import jaeik.growfarm.global.event.MessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * <h2>이벤트 발행 어댑터</h2>
 * <p>
 * Secondary Adapter: 도메인 이벤트를 Spring의 ApplicationEventPublisher로 발행하는 구현
 * 기존 ApplicationEventPublisher 사용법을 완전히 보존
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Component
@RequiredArgsConstructor
public class EventPublisherAdapter implements PublishEventPort {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     * 
     * <p>기존 ApplicationEventPublisher.publishEvent() 메서드를 완전히 위임:</p>
     * <ul>
     *   <li>Spring의 이벤트 발행 메커니즘 그대로 사용</li>
     *   <li>기존의 이벤트 리스너들과 완전 호환</li>
     *   <li>비동기/동기 처리 방식 모두 보존</li>
     *   <li>트랜잭션 경계와의 상호작용 보존</li>
     * </ul>
     */
    @Override
    public void publishMessageEvent(MessageEvent event) {
        eventPublisher.publishEvent(event);
    }
}