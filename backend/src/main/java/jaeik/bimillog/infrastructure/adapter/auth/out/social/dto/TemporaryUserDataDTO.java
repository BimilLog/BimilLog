package jaeik.bimillog.infrastructure.adapter.auth.out.social.dto;

import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.domain.user.entity.Token;
import lombok.Getter;

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
public class TemporaryUserDataDTO {
    public SocialLoginUserData socialLoginUserData;
    public Token token;
    public String fcmToken;

    /**
     * <h3>기본 생성자</h3>
     * <p>Jackson 직렬화/역직렬화를 위한 기본 생성자</p>
     *
     * @since 2.0.0
     * @author Jaeik
     */
    public TemporaryUserDataDTO() {}

    /**
     * <h3>전체 매개변수 생성자</h3>
     * <p>모든 필드를 초기화하는 생성자</p>
     *
     * @param socialLoginUserData 소셜 로그인 사용자 데이터
     * @param token 토큰 정보
     * @param fcmToken FCM 토큰
     * @since 2.0.0
     * @author Jaeik
     */
    public TemporaryUserDataDTO(SocialLoginUserData socialLoginUserData, Token token, String fcmToken) {
        this.socialLoginUserData = socialLoginUserData;
        this.token = token;
        this.fcmToken = fcmToken;
    }

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
    public static TemporaryUserDataDTO fromDomainProfile(SocialLoginPort.SocialUserProfile userProfile, Token token, String fcmToken) {
        SocialLoginUserData socialLoginUserData = SocialLoginUserData.builder()
                .socialId(userProfile.socialId())
                .email(userProfile.email())
                .provider(userProfile.provider())
                .nickname(userProfile.nickname())
                .profileImageUrl(userProfile.profileImageUrl())
                .fcmToken(fcmToken) // FCM 토큰 저장
                .build();

        return new TemporaryUserDataDTO(socialLoginUserData, token, fcmToken);
    }

    /**
     * <h3>인프라 DTO를 도메인 모델로 변환</h3>
     * <p>SignUpService에서 사용되는 변환 메서드</p>
     *
     * @return 소셜 사용자 프로필 도메인 모델
     * @since 2.0.0
     * @author Jaeik
     */
    public SocialLoginPort.SocialUserProfile toDomainProfile() {
        return new SocialLoginPort.SocialUserProfile(
                socialLoginUserData.socialId(),
                socialLoginUserData.email(),
                socialLoginUserData.provider(),
                socialLoginUserData.nickname(),
                socialLoginUserData.profileImageUrl()
        );
    }
}
