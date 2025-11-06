package jaeik.bimillog.out.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * <h2>FCM 메시지 요청 DTO</h2>
 * <p>Firebase Cloud Messaging 서버에 메시지 전송을 요청하기 위한 데이터 전송 객체입니다.</p>
 * <p>Firebase API 규격에 맞는 JSON 구조를 정의하며 필수 필드 검증을 포함합니다.</p>
 * <p>body가 null이거나 빈 문자열일 경우 기본 메시지가 설정됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Builder
public class FcmMessageDTO {
    private boolean validateOnly;           
    
    @NotNull(message = "FCM 메시지 정보는 필수입니다.")
    @Valid
    private FcmMessageDTO.Message message;

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Message {
        @NotNull(message = "알림 정보는 필수입니다.")
        @Valid
        private FcmMessageDTO.Notification notification;
        
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$|^[a-zA-Z0-9_-]{152}$", 
                message = "올바르지 않은 FCM 토큰 형식입니다.")
        private String token;
    }

    @Builder
    @Getter
    public static class Notification {
        @NotBlank(message = "알림 제목은 필수입니다.")
        private String title;
        
        private String body;
        private String image;
        
        public Notification(String title, String body, String image) {
            this.title = title;
            this.body = (body == null || body.trim().isEmpty()) ? "지금 확인해보세요!" : body;
            this.image = image;
        }
        
        /**
         * <h3>기본 메시지와 함께 알림 생성</h3>
         * <p>body가 제공되지 않을 경우 기본 메시지를 사용합니다.</p>
         */
        public static Notification of(String title, String body, String image) {
            return new Notification(title, body, image);
        }
    }
}