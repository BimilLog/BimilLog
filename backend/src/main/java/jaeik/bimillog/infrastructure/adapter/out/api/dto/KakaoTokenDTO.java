package jaeik.bimillog.infrastructure.adapter.out.api.dto;

/**
 * <h2>카카오 OAuth 토큰</h2>
 * <p>카카오 OAuth 2.0 인증 서버로부터 받은 액세스 토큰과 리프레시 토큰을 담는 DTO입니다.</p>
 *
 * @param accessToken  카카오 OAuth 액세스 토큰
 * @param refreshToken 카카오 OAuth 리프레시 토큰
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public record KakaoTokenDTO(String accessToken, String refreshToken) {
    /**
     * <h3>KakaoTokenDTO 정적 팩터리 메서드</h3>
     * <p>액세스 토큰과 리프레시 토큰으로 KakaoTokenDTO 인스턴스를 생성합니다.</p>
     *
     * @param accessToken  카카오 OAuth 액세스 토큰
     * @param refreshToken 카카오 OAuth 리프레시 토큰
     * @return KakaoTokenDTO 인스턴스
     * @author Jaeik
     * @since 2.0.0
     */
    public static KakaoTokenDTO of(String accessToken, String refreshToken) {
        return new KakaoTokenDTO(accessToken, refreshToken);
    }
}