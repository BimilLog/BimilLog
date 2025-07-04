package jaeik.growfarm.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.growfarm.dto.notification.*;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
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

/**
 * <h2>알림 서비스</h2>
 *
 * <p>
 * 사용자에게 실시간 알림을 제공하고, 알림을 데이터베이스에 저장하는 서비스.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
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
     * @since 1.0.0
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
     * @since 1.0.0
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
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationList(CustomUserDetails userDetails) {
        return notificationRepository.findNotificationsByUserIdOrderByLatest(userDetails.getUserId());
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
     * @since 1.0.0
     */
    public void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO) {
        Long userId = userDetails.getUserId();
        List<Long> deleteIds = updateNotificationDTO.getDeletedIds();
        List<Long> readIds = updateNotificationDTO.getReadIds();

        notificationUpdateService.deleteNotifications(deleteIds, userId);
        notificationUpdateService.markNotificationsAsRead(readIds, userId);
    }

/**
     * <h3>FCM 메시지 전송</h3>
     *
     * <p>
     * Firebase Cloud Messaging(FCM)을 통해 푸시 알림을 전송합니다.
     * </p>
     *
     * @param fcmSendDto FCM 전송 정보 DTO
     * @throws IOException FCM 전송 중 발생할 수 있는 예외
     * @author Jaeik
     * @since 1.0.0
     */
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
     * <h3>Firebase Access Token 획득</h3>
     *
     * <p>
     * Firebase Admin SDK를 사용하여 FCM 전송에 필요한 Access Token을 획득합니다.
     * </p>
     *
     * @return String Firebase Access Token
     * @throws IOException Firebase 설정 파일을 읽는 중 발생할 수 있는 예외
     * @author Jaeik
     * @since 1.0.0
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
     * <h3>FCM 메시지 생성</h3>
     *
     * <p>
     * FCM 전송을 위한 메시지 형식으로 변환합니다.
     * </p>
     *
     * @param fcmSendDto FCM 전송 정보 DTO
     * @return String FCM 메시지 JSON 문자열
     * @throws JsonProcessingException JSON 변환 중 발생할 수 있는 예외
     * @author Jaeik
     * @since 1.0.0
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