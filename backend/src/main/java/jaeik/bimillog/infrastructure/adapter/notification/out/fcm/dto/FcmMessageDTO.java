package jaeik.bimillog.infrastructure.adapter.notification.out.fcm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * <h2>FCM 메시지 요청 DTO</h2>
 * <p>
 * Firebase Cloud Messaging 서버에 메시지 전송을 요청하기 위한 데이터 전송 객체입니다.
 * Firebase API 규격에 맞는 JSON 구조를 정의합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
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