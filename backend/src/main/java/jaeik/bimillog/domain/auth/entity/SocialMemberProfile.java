package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>소셜 사용자 프로필</h2>
 * <p>소셜 플랫폼에서 받아온 사용자 프로필 정보를 담는 도메인 값 객체입니다.</p>
 * <p>외부 소셜 플랫폼의 API 응답을 도메인 계층에서 사용할 수 있는 순수한 형태로 변환한 모델입니다.</p>
 * <p>카카오, 구글 등 플랫폼별 차이점을 추상화하여 통일된 인터페이스를 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Getter
@Setter
@AllArgsConstructor
public class SocialMemberProfile {
    String socialId;
    String email;
    SocialProvider provider;
    String nickname;
    String profileImageUrl;
    String kakaoAccessToken;
    String kakaoRefreshToken;
    String fcmToken;

    /**
     * <h3>소셜 회원 프로필 생성</h3>
     * <p>소셜 플랫폼에서 가져온 회원 정보로 프로필 객체를 생성합니다.</p>
     * <p>회원가입 전 Redis에 임시 저장하거나 회원가입 시 Member 엔티티 생성에 사용됩니다.</p>
     *
     * @param socialId 소셜 플랫폼에서 제공하는 고유 ID
     * @param email 소셜 계정 이메일
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param nickname 소셜 플랫폼 닉네임
     * @param profileImageUrl 소셜 플랫폼 프로필 이미지 URL
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @param kakaoRefreshToken 카카오 리프레시 토큰
     * @param fcmToken FCM 푸시 알림 토큰
     * @return 생성된 소셜 회원 프로필 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static SocialMemberProfile of(String socialId, String email, SocialProvider provider, String nickname, String profileImageUrl, String kakaoAccessToken, String kakaoRefreshToken, String fcmToken) {
        return new SocialMemberProfile(socialId, email, provider, nickname, profileImageUrl, kakaoAccessToken, kakaoRefreshToken, fcmToken);
    }
}