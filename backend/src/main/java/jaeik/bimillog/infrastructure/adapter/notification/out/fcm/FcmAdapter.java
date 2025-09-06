package jaeik.bimillog.infrastructure.adapter.notification.out.fcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.infrastructure.adapter.notification.out.fcm.dto.FcmMessageDTO;
import jaeik.bimillog.infrastructure.adapter.notification.out.fcm.dto.FcmSendDTO;
import jaeik.bimillog.infrastructure.adapter.notification.out.persistence.notification.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <h2>FCM 어댑터</h2>
 * <p>
 * Firebase Cloud Messaging(FCM) 토큰 관리 및 메시지 전송을 처리하는 아웃바운드 어댑터입니다.
 * Google Firebase 서비스와의 통신을 담당하며, 푸시 알림 기능을 제공합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class FcmAdapter implements FcmPort {

    private final FcmTokenRepository fcmTokenRepository;

    private static final String API_URL = "https://fcm.googleapis.com/v1/projects/growfarm-6cd79/messages:send";
    private static final String FIREBASE_CONFIG_PATH = "growfarm-6cd79-firebase-adminsdk-fbsvc-ad2bc92194.json";
    private static final String DEFAULT_NOTIFICATION_BODY = "지금 확인해보세요!";
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>주어진 FCM 전송 DTO를 사용하여 메시지를 특정 기기로 전송합니다.</p>
     *
     * @param fcmMessage 전송할 FCM 메시지 정보
     * @throws IOException 메시지 전송 중 발생할 수 있는 IO 예외
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendMessageTo(FcmMessage fcmMessage) throws IOException {
        FcmSendDTO fcmSendDto = toDto(fcmMessage);
        String message = makeMessage(fcmSendDto);
        
        RestTemplate restTemplate = createConfiguredRestTemplate();
        HttpEntity<String> entity = createHttpEntity(message);
        
        restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);
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


    /* ===================== FCM Token Management Methods ===================== */

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
                .createScoped(List.of(FCM_SCOPE));

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

    /**
     * <h3>도메인 FCM 메시지를 DTO로 변환</h3>
     * <p>FcmMessage 도메인 값 객체를 FcmSendDTO로 변환합니다.</p>
     *
     * @param fcmMessage 도메인 FCM 메시지
     * @return FcmSendDTO
     * @author Jaeik
     * @since 2.0.0
     */
    private FcmSendDTO toDto(FcmMessage fcmMessage) {
        return new FcmSendDTO(fcmMessage.token(), fcmMessage.title(), fcmMessage.body());
    }

    private RestTemplate createConfiguredRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
                .addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    private HttpEntity<String> createHttpEntity(String message) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", BEARER_PREFIX + getAccessToken());
        return new HttpEntity<>(message, headers);
    }

}
