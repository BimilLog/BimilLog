package jaeik.growfarm.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import jaeik.growfarm.dto.notification.FcmMessageDto;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <h2>비동기 FCM 알림 서비스</h2>
 * <p>
 * FCM 푸시 알림을 비동기로 처리하는 전용 서비스
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * <h3>댓글 달림 FCM 알림</h3>
     *
     * @param postOwnerId 게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendCommentFcmNotificationAsync(Long postOwnerId, String commenterName) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(postOwnerId);
            if (fcmTokens == null) return;

            for (FcmToken fcmToken : fcmTokens) {
                sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(commenterName + "님이 댓글을 남겼습니다!")
                        .body("지금 확인해보세요!")
                        .build());
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }

    /**
     * <h3>롤링페이퍼에 메시지 수신 FCM 알림</h3>
     *
     * @param paperOwnerId 롤링페이퍼 주인 ID
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendPaperPlantFcmNotificationAsync(Long paperOwnerId) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(paperOwnerId);
            if (fcmTokens == null) return;

            for (FcmToken fcmToken : fcmTokens) {
                sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title("롤링페이퍼에 메시지가 작성되었어요!")
                        .body("지금 확인해보세요!")
                        .build());
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }

    /**
     * <h3>인기글 등극 FCM 알림</h3>
     *
     * @param userId 사용자 ID
     * @param title 알림 제목
     * @param body  알림 내용
     * @author Jaeik
     * @since 1.0.0
     */
    @Async("fcmNotificationExecutor")
    public void sendPostFeaturedFcmNotificationAsync(Long userId, String title, String body) {
        try {
            List<FcmToken> fcmTokens = fcmValidate(userId);
            if (fcmTokens == null) return;

            for (FcmToken fcmToken : fcmTokens) {
                sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(title)
                        .body(body)
                        .build());
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }

    /**
     * <h3>FCM 토큰 유효성 검사</h3>
     * <p>
     * 사용자 설정에 따라 FCM 토큰을 조회하고 유효성을 검사합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 유효한 FCM 토큰 리스트 또는 null
     * @author Jaeik
     * @since 1.0.0
     */
    public List<FcmToken> fcmValidate(Long userId) {
        List<FcmToken> fcmTokens = fcmTokenRepository.findValidFcmTokensByUserId(userId);
        return fcmTokens.isEmpty() ? null : fcmTokens;
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