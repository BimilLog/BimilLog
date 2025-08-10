package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.*;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.read.NotificationReadRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <h2>SSE 알림 서비스</h2>
 *
 * <p>
 * SSE를 통한 실시간 알림 전송 및 알림 데이터 관리를 담당하는 서비스.
 * SRP: SSE 알림 전송과 기본적인 알림 관리만 담당
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationSender {

    private final NotificationReadRepository notificationReadRepository;
    private final EmitterRepository emitterRepository;
    private final NotificationUtil notificationUtil;
    private final UserRepository userRepository;
    private final NotificationUpdateService notificationUpdateService;

    /**
     * <h3>SSE 구독</h3>
     *
     * <p>사용자의 실시간 알림을 위한 SSE 연결을 생성한다.</p>
     *
     * @param tokenId 사용자 토큰 ID
     * @return SSE Emitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public SseEmitter subscribe(Long userId, Long tokenId) {
        String emitterId = notificationUtil.makeTimeIncludeId(userId, tokenId);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(Long.MAX_VALUE));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        sendNotification(emitter, emitterId, NotificationType.INITIATE,
                "EventStream Created. [emitterId=%s]".formatted(emitterId), "");

        return emitter;
    }

    /**
     * <h3>SSE 실시간 알림 발송</h3>
     *
     * <p>
     * 특정 사용자에게 SSE를 통한 실시간 알림을 발송하고 DB에 저장한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param eventDTO 이벤트 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void send(Long userId, EventDTO eventDTO) {
        try {
            NotificationType type = eventDTO.getType();
            String data = eventDTO.getData();
            String url = eventDTO.getUrl();

            Users user = userRepository.getReferenceById(userId);
            notificationUpdateService.saveNotification(user, type, data, url);
            Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);

            emitters.forEach(
                    (emitterId, emitter) -> {
                        sendNotification(emitter, emitterId, type, data, url);
                    });
        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SEND_ERROR, e);
        }
    }

    /**
     * <h3>알림 전송</h3>
     *
     * <p>
     * SSE Emitter를 통해 알림을 전송한다.
     * </p>
     *
     * @param emitter   SSE Emitter 객체
     * @param emitterId Emitter ID
     * @param type      알림 유형
     * @param data      알림 데이터
     * @param url       알림 URL
     * @author Jaeik
     * @since 2.0.0
     */
    private void sendNotification(SseEmitter emitter, String emitterId, NotificationType type, String data, String url) {
        String jsonData = String.format("{\"message\": \"%s\", \"url\": \"%s\"}",
                data, url);
        try {
            emitter.send(SseEmitter.event()
                    .name(type.toString())
                    .data(jsonData));
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
        }
    }

    /**
     * <h3>알림 리스트 조회</h3>
     *
     * <p>
     * 현재 로그인한 사용자의 알림 리스트를 조회한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return List<NotificationDTO> 알림 DTO 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationList(CustomUserDetails userDetails) {
        return notificationReadRepository.findNotificationsByUserIdOrderByLatest(userDetails.getUserId());
    }

    /**
     * <h3>알림 읽음/삭제 처리</h3>
     *
     * <p>
     * 현재 로그인한 사용자의 알림을 읽음 처리하거나 삭제한다.
     * </p>
     * <p>
     * 프론트에서 읽음/삭제 처리해야 할 알림 ID 목록을 모아서 보내준다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param updateNotificationDTO 알림 업데이트 정보
     * @author Jaeik
     * @since 2.0.0
     */
    public void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO) {
        Long userId = userDetails.getUserId();
        List<Long> deleteIds = updateNotificationDTO.getDeletedIds();
        List<Long> readIds = updateNotificationDTO.getReadIds();

        notificationUpdateService.deleteNotifications(deleteIds, userId);
        notificationUpdateService.markNotificationsAsRead(readIds, userId);
    }

    /**
     * <h3>알림 전송 타입 반환</h3>
     *
     * @return SSE 알림 타입
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String getType() {
        return "SSE";
    }
}