package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;

/**
 * <h2>임시 사용자 데이터</h2>
 * <p>소셜 로그인 성공 후 회원가입 완료까지 임시 저장되는 사용자 데이터입니다.</p>
 * <p>Redis에 UUID 키로 저장되어 회원가입 페이지에서 사용자 이름 입력 완료 시 활용됩니다.</p>
 * <p>신규 사용자의 회원가입 프로세스에서 소셜 인증 정보를 안전하게 전달하기 위한 임시 컨테이너입니다.</p>
 *
 * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
 * @param email 사용자 이메일 주소
 * @param provider 소셜 플랫폼 제공자 (KAKAO, GOOGLE 등)
 * @param nickname 소셜 플랫폼에서의 사용자 닉네임
 * @param profileImageUrl 프로필 이미지 URL (선택사항)
 * @param token 소셜 로그인으로 발급받은 토큰 정보
 * @param fcmToken 푸시 알림용 Firebase Cloud Messaging 토큰 (선택사항)
 *
 * @author Jaeik
 * @version 2.0.0
 */
public record TempUserData(
        String socialId,
        String email,
        SocialProvider provider,
        String nickname,
        String profileImageUrl,
        Token token,
        String fcmToken
) {
    /**
     * <h3>소셜 프로필 정보 추출</h3>
     * <p>TempUserData에서 SocialUserProfile 객체를 생성하여 반환합니다.</p>
     * <p>SaveUserPort 등에서 SocialUserProfile이 필요한 경우 사용됩니다.</p>
     *
     * @return SocialUserProfile 객체
     */
    public SocialUserProfile toSocialUserProfile() {
        return new SocialUserProfile(socialId, email, provider, nickname, profileImageUrl);
    }

    /**
     * <h3>팩토리 메서드: SocialUserProfile과 함께 생성</h3>
     * <p>SocialUserProfile과 Token, fcmToken을 받아 TempUserData를 생성합니다.</p>
     * <p>기존 코드와의 호환성을 위한 헬퍼 메서드입니다.</p>
     *
     * @param profile 소셜 사용자 프로필
     * @param token 토큰 정보
     * @param fcmToken FCM 토큰
     * @return TempUserData 객체
     */
    public static TempUserData from(SocialUserProfile profile, Token token, String fcmToken) {
        return new TempUserData(
                profile.socialId(),
                profile.email(),
                profile.provider(),
                profile.nickname(),
                profile.profileImageUrl(),
                token,
                fcmToken
        );
    }
}