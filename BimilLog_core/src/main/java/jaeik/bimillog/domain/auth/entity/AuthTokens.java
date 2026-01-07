package jaeik.bimillog.domain.auth.entity;

/**
 * <h2>인증 토큰 값 객체</h2>
 * <p>액세스/리프레시 토큰 값을 전달하기 위한 도메인 값 객체입니다.</p>
 *
 * @param accessToken JWT 액세스 토큰
 * @param refreshToken JWT 리프레시 토큰
 */
public record AuthTokens(String accessToken, String refreshToken) {}
