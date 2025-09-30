package jaeik.bimillog.domain.user.entity;

import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>임시 사용자 데이터</h2>
 * <p>소셜 로그인 성공 후 회원가입 완료까지 임시 저장되는 사용자 데이터입니다.</p>
 * <p>Redis에 UUID 키로 저장되어 회원가입 페이지에서 사용자 이름 입력 완료 시 활용됩니다.</p>
 * <p>신규 사용자의 회원가입 프로세스에서 소셜 인증 정보를 안전하게 전달하기 위한 임시 컨테이너입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TempUserData {

    private String socialId;
    private String email;
    private SocialProvider provider;
    private String nickname;
    private String profileImageUrl;
    private String kakaoAccessToken;
    private String kakaoRefreshToken;
    private String fcmToken;

    /**
     * <h3>팩토리 메서드: SocialUserProfile로부터 생성</h3>
     * <p>SocialUserProfile을 받아 TempUserData를 생성합니다.</p>
     * <p>SocialUserProfile에 카카오 액세스/리프레시 토큰과 FCM 토큰이 포함되어 있습니다.</p>
     *
     * @param profile 소셜 사용자 프로필 (카카오 토큰, FCM 토큰 포함)
     * @return TempUserData 객체
     */
    public static TempUserData from(SocialUserProfile profile) {
        return new TempUserData(
                profile.getSocialId(),
                profile.getEmail(),
                profile.getProvider(),
                profile.getNickname(),
                profile.getProfileImageUrl(),
                profile.getKakaoAccessToken(),
                profile.getKakaoRefreshToken(),
                profile.getFcmToken()
        );
    }

    /**
     * <h3>SocialUserProfile로 변환</h3>
     * <p>개별 필드를 SocialUserProfile로 재조립합니다.</p>
     *
     * @return SocialUserProfile 객체
     */
    public SocialUserProfile toSocialUserProfile() {
        return SocialUserProfile.of(
                socialId,
                email,
                provider,
                nickname,
                profileImageUrl,
                kakaoAccessToken,
                kakaoRefreshToken,
                fcmToken
        );
    }
}