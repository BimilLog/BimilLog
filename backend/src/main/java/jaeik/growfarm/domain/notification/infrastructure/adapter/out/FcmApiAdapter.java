package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.growfarm.domain.notification.application.port.out.SendFcmPort;
import jaeik.growfarm.dto.notification.FcmMessageDto;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <h2>FCM API 어댑터</h2>
 * <p>Firebase Cloud Messaging(FCM) API를 통해 메시지를 전송하는 Secondary Adapter</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class FcmApiAdapter implements SendFcmPort {

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
