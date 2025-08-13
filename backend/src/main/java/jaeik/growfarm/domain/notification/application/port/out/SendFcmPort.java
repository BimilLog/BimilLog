package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.dto.notification.FcmSendDTO;
import java.io.IOException;

/**
 * <h2>FCM 전송 Port</h2>
 * <p>Firebase Cloud Messaging (FCM) 메시지 전송 기능을 위한 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SendFcmPort {
    /**
     * <h3>FCM 메시지 전송</h3>
     * <p>주어진 FCM 전송 DTO를 사용하여 메시지를 특정 기기로 전송합니다.</p>
     *
     * @param fcmSendDto 전송할 FCM 메시지 정보
     * @throws IOException 메시지 전송 중 발생할 수 있는 IO 예외
     * @author Jaeik
     * @since 2.0.0
     */
    void sendMessageTo(FcmSendDTO fcmSendDto) throws IOException;
}
