package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.LoadFcmTokenPort;
import jaeik.growfarm.domain.notification.application.port.out.NotificationSender;
import jaeik.growfarm.domain.notification.application.port.out.SendFcmPort;
import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FcmNotificationSender implements NotificationSender {

    private final LoadFcmTokenPort loadFcmTokenPort;
    private final SendFcmPort sendFcmPort;

    @Override
    @Async("fcmNotificationExecutor")
    public void send(Long userId, EventDTO eventDTO) {
        try {
            List<FcmToken> fcmTokens = loadFcmTokenPort.findValidFcmTokensByUserId(userId);
            if (fcmTokens == null || fcmTokens.isEmpty()) return;

            for (FcmToken fcmToken : fcmTokens) {
                sendFcmPort.sendMessageTo(FcmSendDTO.builder()
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