package jaeik.growfarm.infrastructure.adapter.notification.out.fcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.growfarm.domain.notification.application.port.out.FcmPort;
import jaeik.growfarm.domain.notification.application.port.out.NotificationSender;
import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.infrastructure.adapter.notification.out.fcm.dto.FcmMessageDTO;
import jaeik.growfarm.infrastructure.adapter.notification.out.fcm.dto.FcmSendDTO;
import jaeik.growfarm.infrastructure.adapter.notification.out.persistence.notification.FcmTokenRepository;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.EventDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <h2>FCM 어댑터</h2>
 * <p>FCM 토큰 관련 </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class FcmAdapter implements FcmPort, NotificationSender {

    private final FcmTokenRepository fcmTokenRepository;

    private static final String API_URL = "https://fcm.googleapis.com/v1/projects/growfarm-6cd79/messages:send";
    private static final String FIREBASE_CONFIG_PATH = "growfarm-6cd79-firebase-adminsdk-fbsvc-ad2bc92194.json";

    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>주어진 FCM 전송 DTO를 사용하여 메시지를 특정 기기로 전송합니다.</p>
     *
     * @param fcmSendDto 전송할 FCM 메시지 정보
     * @throws IOException 메시지 전송 중 발생할 수 있는 IO 예외
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendMessageTo(FcmSendDTO fcmSendDto) throws IOException {

        String message = makeMessage(fcmSendDto);
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getMessageConverters()
                .addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getAccessToken());

        HttpEntity<String> entity = new HttpEntity<>(message, headers);

        restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
    }

    /**
     * <h3>FCM 알림 전송</h3>
     * <p>지정된 사용자에게 FCM 알림을 비동기적으로 전송합니다.</p>
     *
     * @param userId 알림을 받을 사용자의 ID
     * @param eventDTO 전송할 이벤트 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Async("fcmNotificationExecutor")
    public void send(Long userId, EventDTO eventDTO) {
        try {
            List<FcmToken> fcmTokens = findValidFcmTokensByUserId(userId);
            if (fcmTokens == null || fcmTokens.isEmpty()) return;

            for (FcmToken fcmToken : fcmTokens) {
                sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(eventDTO.getData())
                        .body("지금 확인해보세요!")
                        .build());
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }

    /**
     * <h3>FCM 토큰 저장</h3>
     * <p>FCM 토큰 엔티티를 저장합니다.</p>
     *
     * @param fcmToken 저장할 FCM 토큰 엔티티
     * @return 저장된 FCM 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public FcmToken save(FcmToken fcmToken) {
        return fcmTokenRepository.save(fcmToken);
    }

    /**
     * <h3>사용자 ID로 유효한 FCM 토큰 조회</h3>
     * <p>주어진 사용자 ID에 해당하는 유효한 FCM 토큰 목록을 조회합니다.</p>
     *
     * @param userId 조회할 사용자의 ID
     * @return FCM 토큰 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<FcmToken> findValidFcmTokensByUserId(Long userId) {
        return fcmTokenRepository.findValidFcmTokensByUserId(userId);
    }

    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByUserId(Long userId) {
        fcmTokenRepository.deleteByUser_Id(userId);
    }

    /**
     * <h3>액세스 토큰 획득</h3>
     * <p>Firebase 서비스 계정 자격 증명을 사용하여 FCM API 호출에 필요한 액세스 토큰을 획득합니다.</p>
     *
     * @return FCM API 호출에 사용될 액세스 토큰
     * @throws IOException Firebase 구성 파일 읽기 중 발생할 수 있는 IO 예외
     * @author Jaeik
     * @since 2.0.0
     */
    private String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(FIREBASE_CONFIG_PATH).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    /**
     * <h3>FCM 메시지 구성</h3>
     * <p>FCM 전송 DTO를 기반으로 FCM API에 전송할 메시지 JSON 문자열을 생성합니다.</p>
     *
     * @param fcmSendDto FCM 전송 DTO
     * @return FCM API로 전송할 JSON 형식의 메시지 문자열
     * @throws JsonProcessingException JSON 변환 중 발생할 수 있는 예외
     * @author Jaeik
     * @since 2.0.0
     */
    private String makeMessage(FcmSendDTO fcmSendDto) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        FcmMessageDTO fcmMessageDto = FcmMessageDTO.builder()
                .message(FcmMessageDTO.Message.builder()
                        .token(fcmSendDto.getToken())
                        .notification(FcmMessageDTO.Notification.builder()
                                .title(fcmSendDto.getTitle())
                                .body(fcmSendDto.getBody())
                                .image(null)
                                .build())
                        .build())
                .validateOnly(false).build();
        return om.writeValueAsString(fcmMessageDto);
    }
}
