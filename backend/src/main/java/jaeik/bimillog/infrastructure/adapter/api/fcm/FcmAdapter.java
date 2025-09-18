package jaeik.bimillog.infrastructure.adapter.api.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import jaeik.bimillog.domain.notification.application.port.out.FcmPort;
import jaeik.bimillog.domain.notification.entity.FcmMessage;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.infrastructure.adapter.api.dto.FcmMessageDTO;
import jaeik.bimillog.infrastructure.adapter.notification.out.jpa.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

/**
 * <h2>FCM 어댑터</h2>
 * <p>Firebase Cloud Messaging 연동을 담당하는 어댑터입니다.</p>
 * <p>FCM 토큰 저장/삭제, FCM 메시지 전송</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class FcmAdapter implements FcmPort {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmApiClient fcmApiClient;

    private static final String FIREBASE_CONFIG_PATH = "growfarm-6cd79-firebase-adminsdk-fbsvc-ad2bc92194.json";
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>Firebase Cloud Messaging API를 통해 모바일 기기로 푸시 알림을 전송합니다.</p>
     * <p>NotificationGenerateListener에서 도메인 이벤트 처리 시 호출되어 실시간 알림을 제공합니다.</p>
     *
     * @param fcmMessage 전송할 FCM 메시지 정보 (토큰, 제목, 내용 포함)
     * @throws IOException Firebase API 호출 중 발생할 수 있는 IO 예외
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void sendMessageTo(FcmMessage fcmMessage) throws IOException {
        FcmMessageDTO fcmMessageDto = createFcmMessageDTO(fcmMessage);
        String accessToken = getAccessToken();
        
        fcmApiClient.sendMessage(
                BEARER_PREFIX + accessToken,
                MediaType.APPLICATION_JSON_VALUE,
                fcmMessageDto
        );
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
     * <p>사용자 탈퇴나 로그아웃 시 해당 사용자의 모든 FCM 토큰을 삭제합니다.</p>
     * <p>FcmTokenRemoveListener에서 Auth 도메인 이벤트 처리 시 호출되어 개인정보를 보호합니다.</p>
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
     * <h3>FCM 메시지 DTO 생성</h3>
     * <p>FCM 전송 DTO를 생성합니다.</p>
     * <p>DTO에서 필드 검증 및 기본값 처리가 완료되므로 단순 변환 작업만 수행합니다.</p>
     *
     * @param fcmMessage FCM 객체
     * @return FcmMessageDTO FCM API로 전송할 메시지 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    private FcmMessageDTO createFcmMessageDTO(FcmMessage fcmMessage) {
        return FcmMessageDTO.builder()
                .message(FcmMessageDTO.Message.builder()
                        .token(fcmMessage.token())
                        .notification(FcmMessageDTO.Notification.of(
                                fcmMessage.title(),
                                fcmMessage.body(),
                                null))
                        .build())
                .validateOnly(false)
                .build();
    }

}
