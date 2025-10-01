package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>소셜 사용자 프로필</h2>
 * <p>소셜 플랫폼에서 받아온 사용자 프로필 정보를 담는 도메인 값 객체입니다.</p>
 * <p>외부 소셜 플랫폼의 API 응답을 도메인 계층에서 사용할 수 있는 순수한 형태로 변환한 모델입니다.</p>
 * <p>카카오, 구글 등 플랫폼별 차이점을 추상화하여 통일된 인터페이스를 제공합니다.</p>
 *
 * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
 * @param email 사용자 이메일 주소
 * @param provider 소셜 플랫폼 제공자 (KAKAO, GOOGLE 등)
 * @param nickname 소셜 플랫폼에서의 사용자 닉네임
 * @param profileImageUrl 프로필 이미지 URL (선택사항)
 * @param kakaoAccessToken 카카오 OAuth 액세스 토큰
 * @param kakaoRefreshToken 카카오 OAuth 리프레시 토큰
 * @param fcmToken Firebase Cloud Messaging 토큰 (선택사항)
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

    public static SocialMemberProfile of(String socialId, String email, SocialProvider provider, String nickname, String profileImageUrl, String kakaoAccessToken, String kakaoRefreshToken, String fcmToken) {
        return new SocialMemberProfile(socialId, email, provider, nickname, profileImageUrl, kakaoAccessToken, kakaoRefreshToken, fcmToken);
    }
}