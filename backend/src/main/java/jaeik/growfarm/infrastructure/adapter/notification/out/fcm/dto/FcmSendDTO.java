package jaeik.growfarm.infrastructure.adapter.notification.out.fcm.dto;

import lombok.*;

/**
 * <h2>FCM 전송 DTO</h2>
 * <p>
 * Firebase Cloud Messaging(FCM)을 통해 알림을 전송하기 위한 데이터 전송 객체입니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmSendDTO {

    private String token;

    private String title;

    private String body;

    @Builder(toBuilder = true)
    public FcmSendDTO(String token, String title, String body) {
        this.token = token;
        this.title = title;
        this.body = body;
    }
}
