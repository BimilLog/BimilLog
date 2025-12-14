package jaeik.bimillog.domain.notification.service;

import jaeik.bimillog.domain.notification.controller.NotificationSseController;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.repository.SseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE 실시간 알림 서비스</h2>
 * <p>Server-Sent Events를 통한 실시간 알림을 담당하는 서비스입니다.</p>
 * <p>SSE 구독 관리, 연결 관리, 이벤트 기반 알림 전송</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SseService {
    private final SseRepository sseRepository;
    private final UrlGenerator urlGenerator;

    /**
     * <h3>SSE 구독</h3>
     * <p>클라이언트의 SSE 구독 요청을 처리하여 실시간 알림 수신을 위한 SseEmitter를 생성합니다.</p>
     * <p>다중 기기 지원을 위해 사용자 ID와 토큰 ID 조합으로 고유한 연결 식별자를 관리합니다.</p>
     * <p>{@link NotificationSseController}에서 클라이언트의 SSE 구독 API 요청을 처리하기 위해 호출됩니다.</p>
     *
     * @param memberId 구독할 사용자 ID
     * @param tokenId 구독 토큰 ID (다중 기기 구분용)
     * @return SseEmitter 객체 (무제한 타임아웃, 로그아웃 시 명시적으로 종료)
     * @author Jaeik
     * @since 2.0.0
     */
    public SseEmitter subscribe(Long memberId, Long tokenId) {
        return sseRepository.subscribe(memberId, tokenId);
    }

    /**
     * <h3>SSE 연결 정리</h3>
     * <p>tokenId가 null인 경우 모든 SSE 연결을 정리하고, 값이 있는 경우 특정 기기만 정리합니다.</p>
     * <p>사용자 탈퇴 시에는 tokenId를 null로 전달하여 모든 연결을 정리하고,</p>
     * <p>개별 기기 로그아웃 시에는 tokenId를 전달하여 해당 기기만 연결 해제합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId 토큰 ID (null인 경우 모든 연결 정리)
     * @author Jaeik
     * @since 2.0.0
     */
    public void deleteEmitters(Long memberId, Long tokenId) {
        sseRepository.deleteEmitters(memberId, tokenId);
    }


    public void sendNotification(Long memberId, NotificationType type, String message, String url) {
        SseMessage sseMessage = SseMessage.of(memberId, type, message, url);
        sseRepository.send(sseMessage);
    }
}
