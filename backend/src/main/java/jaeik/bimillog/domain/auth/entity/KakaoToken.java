package jaeik.bimillog.domain.auth.entity;

/**
 * <h2>카카오 토큰</h2>
 *
 * @param accessToken  카카오 OAuth 액세스 토큰
 * @param refreshToken 카카오 OAuth 리프레시 토큰
 * @author Jaeik
 * @version 2.0.0
 */
public record KakaoToken(String accessToken, String refreshToken) {
    /**
     * <h3>KakaoToken 정적 팩터리 메서드</h3>
     * <p>액세스 토큰과 리프레시 토큰으로 KakaoToken 인스턴스를 생성합니다.</p>
     *
     * @param accessToken  카카오 OAuth 액세스 토큰
     * @param refreshToken 카카오 OAuth 리프레시 토큰
     * @return KakaoToken 인스턴스
     * @author Jaeik
     * @since 2.0.0
     */
    public static KakaoToken of(String accessToken, String refreshToken) {
        return new KakaoToken(accessToken, refreshToken);
    }
}