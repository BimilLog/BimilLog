package jaeik.bimillog.infrastructure.adapter.in.auth.dto;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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