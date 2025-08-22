package jaeik.growfarm.domain.user.entity;

import lombok.Builder;

/**
 * <h3>토큰 값 객체</h3>
 * <p>
 * 카카오 액세스 토큰과 리프레시 토큰 정보를 담는 도메인 값 객체
 * </p>
 *
 * @param accessToken 액세스 토큰
 * @param refreshToken 리프레시 토큰
 * @author Jaeik
 * @since 2.0.0
 */
public record TokenVO(String accessToken, String refreshToken) {

    @Builder
    public TokenVO {
    }

    /**
     * <h3>토큰 값 객체 생성</h3>
     * <p>액세스 토큰과 리프레시 토큰으로 TokenValue를 생성합니다.</p>
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return TokenValue 객체
     */
    public static TokenVO of(String accessToken, String refreshToken) {
        return new TokenVO(accessToken, refreshToken);
    }
}