package jaeik.growfarm.service;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmitterRepository emitterRepository;
    private final NotificationUtil notificationUtil;
    private final UserRepository userRepository;

    public SseEmitter subscribe(Long userId) {
        String emitterId = notificationUtil.makeTimeIncludeId(userId);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(Long.MAX_VALUE));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        sendNotification(emitter, emitterId, NotificationType.INITIATE,
                "EventStream Created. [userId=%d]".formatted(userId), "");

        return emitter;
    }

    public void send(Long userId, EventDTO eventDTO) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        NotificationType type = eventDTO.getType();
        String data = eventDTO.getData();
        String url = eventDTO.getUrl();

        notificationRepository.save(createNotification(user, type, data, url));
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);

        emitters.forEach(
                (emitterId, emitter) -> {
                    sendNotification(emitter, emitterId, type, data, url);
                });
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

    private Notification createNotification(Users user, NotificationType notificationType, String data, String url) {
        return Notification.builder()
                .users(user)
                .notificationType(notificationType)
                .data(data)
                .url(url)
                .isRead(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationList(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findByUsers(user, pageable);

        return notificationPage.getContent().stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }


    @Transactional
    public void batchUpdate(UpdateNotificationDTO updateNotificationDTO) {
        List<Long> deleteIds = updateNotificationDTO.getDeletedIds();
        List<Long> readIds = updateNotificationDTO.getReadIds();

        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationRepository.deleteAllByIdInBatch(deleteIds);
        }

        if (readIds != null && !readIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllById(readIds);
            for (Notification notification : notifications) {
                notification.markAsRead();
            }
        }
    }
}