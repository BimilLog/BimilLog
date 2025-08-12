package jaeik.growfarm.dto.user;

import lombok.Builder;

/**
 * <h3>토큰 정보 DTO</h3>
 * <p>
 * 카카오 토큰과 JWT 토큰 정보를 담는 데이터 전송 객체
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
public record TokenDTO(String accessToken, String refreshToken) {

    @Builder
    public TokenDTO {
    }
}