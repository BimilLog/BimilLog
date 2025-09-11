package jaeik.bimillog.infrastructure.adapter.auth.dto;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/**
 * <h2>소셜 로그인 요청 DTO</h2>
 * <p>소셜 로그인 API 요청을 위한 DTO입니다.</p>
 * <p>provider, code 유효성 검증, FCM 토큰 형식 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequestDTO {

    @NotBlank(message = "소셜 제공자는 필수입니다.")
    private String provider;

    @NotBlank(message = "인증 코드는 필수입니다.")
    private String code;

    private String fcmToken;

    /**
     * <h3>소셜 제공자 유효성 검증</h3>
     * <p>provider가 유효한 SocialProvider 열거형 값인지 검증합니다.</p>
     *
     * @return provider가 유효하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "유효하지 않은 소셜 제공자입니다.")
    public boolean isProviderValid() {
        if (provider == null) return false;
        try {
            SocialProvider.valueOf(provider.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * <h3>FCM 토큰 형식 검증</h3>
     * <p>FCM 토큰이 있을 경우 기본적인 형식을 검증합니다.</p>
     * <p>FCM 토큰이 null인 경우는 유효한 것으로 처리합니다.</p>
     *
     * @return FCM 토큰이 유효하거나 null이면 true, 형식이 잘못되면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "FCM 토큰 형식이 올바르지 않습니다.")
    public boolean isFcmTokenValid() {
        if (fcmToken == null || fcmToken.trim().isEmpty()) return true;
        return fcmToken.matches("^[A-Za-z0-9\\-_:]+$") && fcmToken.length() > 10;
    }

    /**
     * <h3>검증된 소셜 제공자 반환</h3>
     * <p>유효성 검증이 완료된 provider 문자열을 SocialProvider 열거형으로 변환하여 반환합니다.</p>
     * <p>DTO 검증이 통과한 후에만 호출해야 합니다.</p>
     *
     * @return 변환된 SocialProvider 열거형
     * @author Jaeik
     * @since 2.0.0
     */
    public SocialProvider getSocialProvider() {
        return SocialProvider.valueOf(provider.toUpperCase());
    }
}