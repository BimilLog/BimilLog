package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.FcmPort;
import jaeik.growfarm.domain.notification.application.port.out.NotificationSender;
import jaeik.growfarm.domain.notification.entity.FcmToken;
import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>FCM 알림 전송기</h2>
 * <p>Firebase Cloud Messaging(FCM)을 통해 사용자에게 알림을 전송하는 Secondary Adapter</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class FcmNotificationSender implements NotificationSender {

    private final FcmPort fcmPort;

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
            List<FcmToken> fcmTokens = fcmPort.findValidFcmTokensByUserId(userId);
            if (fcmTokens == null || fcmTokens.isEmpty()) return;

            for (FcmToken fcmToken : fcmTokens) {
                fcmPort.sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(eventDTO.getData())
                        .body("지금 확인해보세요!")
                        .build());
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FCM_SEND_ERROR, e);
        }
    }
}