package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.growfarm.domain.notification.application.port.out.SsePort;
import jaeik.growfarm.domain.notification.application.port.out.NotificationSender;
import jaeik.growfarm.domain.notification.application.service.NotificationUrlGenerator;
import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.EventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>알림 구독 서비스</h2>
 * <p>SSE 실시간 알림 구독 관련 비즈니스 로직을 처리하는 Use Case 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationSseService implements NotificationSseUseCase {

    private final SsePort ssePort;
    private final NotificationSender notificationSender;
    private final NotificationUrlGenerator urlGenerator;
    private final NotificationUtilPort notificationUtilPort;

    /**
     * <h3>알림 구독</h3>
     * <p>주어진 사용자 ID와 토큰 ID로 SSE 알림을 구독합니다.</p>
     *
     * @param userId 구독할 사용자의 ID
     * @param tokenId 구독 토큰 ID
     * @return SseEmitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SseEmitter subscribe(Long userId, Long tokenId) {
        return ssePort.subscribe(userId, tokenId);
    }

    /**
     * <h3>사용자 SSE 연결 정리</h3>
     * <p>사용자와 관련된 모든 SSE Emitter 연결을 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        ssePort.deleteAllEmitterByUserId(userId);
    }

    /**
     * <h3>댓글 알림 SSE 전송</h3>
     *
     * @param postUserId    게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId        게시글 ID
     */
    @Override
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        String message = commenterName + "님이 댓글을 남겼습니다!";
        String url = urlGenerator.generatePostUrl(postId);
        EventDTO eventDTO = notificationUtilPort.createEventDTO(NotificationType.COMMENT, message, url);
        notificationSender.send(postUserId, eventDTO);
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 SSE 전송</h3>
     *
     * @param farmOwnerId 롤링페이퍼 주인 ID
     * @param userName    사용자 이름
     */
    @Override
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        String message = "롤링페이퍼에 메시지가 작성되었어요!";
        String url = urlGenerator.generatePaperUrl(userName);
        EventDTO eventDTO = notificationUtilPort.createEventDTO(NotificationType.PAPER, message, url);
        notificationSender.send(farmOwnerId, eventDTO);
    }

    /**
     * <h3>인기글 등극 알림 SSE 전송</h3>
     *
     * @param userId  사용자 ID
     * @param message 알림 메시지
     * @param postId  게시글 ID
     */
    @Override
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        String url = urlGenerator.generatePostUrl(postId);
        EventDTO eventDTO = notificationUtilPort.createEventDTO(NotificationType.POST_FEATURED, message, url);
        notificationSender.send(userId, eventDTO);
    }
}