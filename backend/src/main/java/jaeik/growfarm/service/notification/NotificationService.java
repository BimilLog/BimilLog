package jaeik.growfarm.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.growfarm.dto.notification.*;
import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * NotificationService 클래스
 * 알림 관련 서비스 클래스
 * 수정일 : 2025-05-03
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmitterRepository emitterRepository;
    private final NotificationUtil notificationUtil;
    private final UserRepository userRepository;

    /**
     * <h3>SSE 구독</h3>
     *
     * <p>사용자의 실시간 알림을 위한 SSE 연결을 생성한다.</p>
     *
     * @param tokenId 사용자 토큰 ID
     * @return SSE Emitter 객체
     * @author Jaeik
     * @since 1.0.0
     */
    public SseEmitter subscribe(Long userId, Long tokenId) {
        String emitterId = notificationUtil.makeTimeIncludeId(userId, tokenId);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(Long.MAX_VALUE));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        sendNotification(emitter, emitterId, NotificationType.INITIATE,
                "EventStream Created. [tokenId=%d]".formatted(emitterId), "");

        return emitter;
    }

    /**
     * <h3>실시간 알림 발송</h3>
     *
     * <p>
     * 특정 사용자에게 실시간 알림을 발송하고 DB에 저장한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param eventDTO 이벤트 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
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

    private void sendNotification(SseEmitter emitter, String emitterId, NotificationType type, String data,
                                  String url) {
        String jsonData = String.format("{\"paper\": \"%s\", \"url\": \"%s\"}",
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

    public void sendMessageTo(FcmSendDTO fcmSendDto) throws IOException {

        String message = makeMessage(fcmSendDto);
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getMessageConverters()
                .addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getAccessToken());

        HttpEntity<String> entity = new HttpEntity<>(message, headers);

        String API_URL = "https://fcm.googleapis.com/v1/projects/growfarm-6cd79/messages:send";
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

        System.out.println(response.getStatusCode());
    }

    /**
     * Firebase Admin SDK의 비공개 키를 참조하여 Bearer 토큰을 발급 받습니다.
     *
     * @return Bearer token
     */
    private String getAccessToken() throws IOException {
        String firebaseConfigPath = "firebase/growfarm-6cd79-firebase-adminsdk-fbsvc-7d4ebe98d2.json";

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    /**
     * FCM 전송 정보를 기반으로 메시지를 구성합니다. (Object -> String)
     *
     * @param fcmSendDto FcmSendDto
     * @return String
     */
    private String makeMessage(FcmSendDTO fcmSendDto) throws JsonProcessingException {

        ObjectMapper om = new ObjectMapper();
        FcmMessageDto fcmMessageDto = FcmMessageDto.builder()
                .message(FcmMessageDto.Message.builder()
                        .token(fcmSendDto.getToken())
                        .notification(FcmMessageDto.Notification.builder()
                                .title(fcmSendDto.getTitle())
                                .body(fcmSendDto.getBody())
                                .image(null)
                                .build())
                        .build())
                .validateOnly(false).build();

        return om.writeValueAsString(fcmMessageDto);
    }
}