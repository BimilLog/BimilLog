package jaeik.growfarm.service.notification;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>FCM 알림 전송자</h2>
 * <p>
 * FCM을 통한 푸시 알림 전송을 담당하는 서비스
 * SRP: FCM 푸시 알림 전송만 담당
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmNotificationSender implements NotificationSender {

    private final FcmService fcmService;

    /**
     * <h3>FCM 푸시 알림 발송</h3>
     *
     * <p>
     * 특정 사용자에게 FCM을 통한 푸시 알림을 발송한다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param eventDTO 이벤트 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    @Async("fcmNotificationExecutor")
    public void send(Long userId, EventDTO eventDTO) {
        try {
            List<FcmToken> fcmTokens = fcmService.fcmValidate(userId);
            if (fcmTokens == null) return;

            for (FcmToken fcmToken : fcmTokens) {
                fcmService.sendMessageTo(FcmSendDTO.builder()
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
     * <h3>알림 전송 타입 반환</h3>
     *
     * @return FCM 알림 타입
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public String getType() {
        return "FCM";
    }
}