package jaeik.growfarm.infrastructure.adapter.auth.out.social.dto;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.user.entity.TokenVO;
import lombok.Getter;

@Getter
public class TemporaryUserDataDTO {
    public SocialLoginUserData socialLoginUserData;
    public TokenVO tokenVO;
    public String fcmToken;

    public TemporaryUserDataDTO(SocialLoginUserData socialLoginUserData, TokenVO tokenVO, String fcmToken) {
        this.socialLoginUserData = socialLoginUserData;
        this.tokenVO = tokenVO;
        this.fcmToken = fcmToken;
    }

    /**
     * 도메인 모델에서 인프라 DTO로 변환하는 정적 팩토리 메서드
     * (의존성 역전 원칙을 준수하면서 어댑터에서 변환 처리)
     */
    public static TemporaryUserDataDTO fromDomainProfile(SocialLoginPort.SocialUserProfile userProfile, TokenVO tokenVO, String fcmToken) {
        SocialLoginUserData socialLoginUserData = SocialLoginUserData.builder()
                .socialId(userProfile.socialId())
                .email(userProfile.email())
                .provider(userProfile.provider())
                .nickname(userProfile.nickname())
                .profileImageUrl(userProfile.profileImageUrl())
                .fcmToken(fcmToken) // FCM 토큰 저장
                .build();

        return new TemporaryUserDataDTO(socialLoginUserData, tokenVO, fcmToken);
    }

    /**
     * 인프라 DTO를 도메인 모델로 변환하는 메서드
     * (SignUpService에서 사용)
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
