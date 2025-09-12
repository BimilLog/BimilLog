package jaeik.bimillog.infrastructure.adapter.auth.dto;

import jaeik.bimillog.domain.auth.entity.SocialAuthData;
import jaeik.bimillog.domain.user.entity.Token;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.AssertTrue;


/**
 * <h2>임시 사용자 데이터 DTO</h2>
 * <p>Redis 저장을 위한 인프라 계층 DTO</p>
 * <p>도메인 모델인 TempUserData와 구분되며, 직렬화/역직렬화를 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryUserDataDTO {
    private SocialAuthData.SocialUserProfile socialUserProfile;
    private Token token;
    private String fcmToken;


    /**
     * <h3>도메인 모델에서 인프라 DTO로 변환</h3>
     * <p>의존성 역전 원칙을 준수하면서 어댑터에서 변환 처리</p>
     *
     * @param userProfile 소셜 사용자 프로필 도메인 모델
     * @param token 토큰 정보
     * @param fcmToken FCM 토큰
     * @return 인프라 DTO 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static TemporaryUserDataDTO fromDomainProfile(SocialAuthData.SocialUserProfile userProfile, Token token, String fcmToken) {
        return new TemporaryUserDataDTO(userProfile, token, fcmToken);
    }

    /**
     * <h3>인프라 DTO를 도메인 모델로 변환</h3>
     * <p>SignUpService에서 사용되는 변환 메서드</p>
     * <p>필수 데이터의 null 체크를 수행하여 안전한 도메인 객체 변환을 보장합니다.</p>
     *
     * @return 소셜 사용자 프로필 도메인 모델
     * @throws IllegalArgumentException socialUserProfile이나 token이 null인 경우
     * @since 2.0.0
     * @author Jaeik
     */
    public SocialAuthData.SocialUserProfile toDomainProfile() {
        if (socialUserProfile == null) {
            throw new IllegalArgumentException("socialUserProfile은 null이 될 수 없습니다");
        }
        if (token == null) {
            throw new IllegalArgumentException("token은 null이 될 수 없습니다");
        }
        return socialUserProfile;
    }

    /**
     * <h3>필수 데이터 검증</h3>
     * <p>socialUserProfile과 token이 null이 아님을 보장합니다.</p>
     * <p>Redis에서 역직렬화된 데이터의 무결성을 검증하여 안전한 도메인 변환을 지원합니다.</p>
     *
     * @return 필수 데이터가 모두 존재하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @AssertTrue(message = "socialUserProfile과 token은 필수입니다")
    private boolean isValidData() {
        return socialUserProfile != null && token != null;
    }
}
