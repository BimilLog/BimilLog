package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.dto.notification.EventDTO;

/**
 * <h2>알림 전송 인터페이스</h2>
 * <p>
 * 다양한 알림 전송 방식을 추상화하는 인터페이스
 * </p>
 * <p>
 * SRP: 알림 전송이라는 단일 책임만 담당
 * OCP: 새로운 알림 방식 추가 시 인터페이스 변경 없이 확장 가능
 * LSP: 모든 구현체는 동일한 계약을 준수
 * ISP: 알림 전송에 필요한 최소한의 메서드만 정의
 * DIP: 구체적인 구현이 아닌 추상화에 의존
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationSender {

    /**
     * <h3>알림 전송</h3>
     * <p>
     * 사용자에게 알림을 전송합니다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param eventDTO 이벤트 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    void send(Long userId, EventDTO eventDTO);
}