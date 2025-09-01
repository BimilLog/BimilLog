package jaeik.bimillog.infrastructure.adapter.notification.out.fcm.dto;

import lombok.*;

/**
 * <h2>FCM 전송 DTO</h2>
 * <p>
 * Firebase Cloud Messaging(FCM)을 통해 알림을 전송하기 위한 데이터 전송 객체입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmSendDTO {

    private String token;
    private String title;
    private String body;

    /**
     * <h3>FCM 전송 DTO 생성자</h3>
     * <p>모든 필드를 초기화하는 생성자</p>
     *
     * @param token FCM 토큰
     * @param title 알림 제목
     * @param body  알림 내용
     * @author Jaeik
     * @since 2.0.0
     */
    public FcmSendDTO(String token, String title, String body) {
        this.token = token;
        this.title = title;
        this.body = body;
    }
}
