package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.user.entity.Token;

/**
 * <h2>임시 사용자 데이터</h2>
 * <p>회원가입 프로세스에서 사용하는 순수 도메인 모델</p>
 * <p>소셜 로그인 후 회원가입 완료까지의 임시 데이터를 담고 있습니다.</p>
 *
 * @param userProfile 소셜 사용자 프로필 정보
 * @param token 소셜 로그인 토큰 정보
 * @param fcmToken FCM 푸시 알림 토큰 (선택적)
 * 
 * @author Jaeik
 * @version 2.0.0
 */
public record TempUserData(
        SocialUserProfile userProfile,
        Token token,
        String fcmToken
) {
    /**
     * <h3>임시 사용자 데이터 생성</h3>
     * <p>FCM 토큰 검증 후 임시 데이터 객체를 생성합니다.</p>
     *
     * @param userProfile 소셜 사용자 프로필 (필수)
     * @param token 토큰 정보 (필수)  
     * @param fcmToken FCM 토큰 (선택적)
     * @return 검증된 임시 사용자 데이터
     * @throws IllegalArgumentException userProfile이나 token이 null인 경우
     * @since 2.0.0
     */
    public static TempUserData of(SocialUserProfile userProfile, Token token, String fcmToken) {
        if (userProfile == null) {
            throw new IllegalArgumentException("유저 프로파일은 null이 될 수 없습니다");
        }
        if (token == null) {
            throw new IllegalArgumentException("token은 null이 될 수 없습니다");
        }
        return new TempUserData(userProfile, token, fcmToken);
    }
}