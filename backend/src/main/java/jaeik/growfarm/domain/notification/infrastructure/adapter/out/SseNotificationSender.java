package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.notification.NotificationSender;
import jaeik.growfarm.service.notification.NotificationUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * <h2>SSE 알림 전송자</h2>
 * <p>SSE를 통한 실시간 알림 전송을 담당하는 서비스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SseNotificationSender implements NotificationSender {

    private final EmitterRepository emitterRepository;
    private final UserRepository userRepository;
    private final NotificationUpdateService notificationUpdateService;

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

    @Override
    public String getType() {
        return "SSE";
    }
}