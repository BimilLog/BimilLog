package jaeik.bimillog.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>FCM 토큰 등록 요청 DTO</h2>
 * <p>
 * FCM 푸시 알림 토큰 등록 API 요청에 사용되는 데이터 전송 객체입니다.
 * 클라이언트가 Firebase에서 발급받은 FCM 토큰을 서버에 등록할 때 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRegisterRequestDTO {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String fcmToken;

}
