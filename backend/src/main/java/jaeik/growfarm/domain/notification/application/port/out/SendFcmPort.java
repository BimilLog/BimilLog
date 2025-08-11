package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.dto.notification.FcmSendDTO;
import java.io.IOException;

public interface SendFcmPort {
    void sendMessageTo(FcmSendDTO fcmSendDto) throws IOException;
}
