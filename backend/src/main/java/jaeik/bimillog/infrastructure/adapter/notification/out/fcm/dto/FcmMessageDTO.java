package jaeik.bimillog.infrastructure.adapter.notification.out.fcm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// FCM 서버에 메시지 요청용 DTO
@Getter
@Builder
public class FcmMessageDTO {
    private boolean validateOnly;           
    private FcmMessageDTO.Message message;

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Message {
        private FcmMessageDTO.Notification notification;
        private String token;
    }

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Notification {
        private String title;
        private String body;
        private String image;
    }
}