package jaeik.growfarm.dto.notification;

import lombok.*;

// FCM 메시지 생성용 DTO
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
