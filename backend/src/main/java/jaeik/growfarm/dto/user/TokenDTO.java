package jaeik.growfarm.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * <h3>토큰 정보 DTO</h3>
 * <p>
 * 카카오 토큰과 JWT 토큰 정보를 담는 데이터 전송 객체
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Getter
public class TokenDTO {

    private final String accessToken;
    private final String refreshToken;

    @Builder
    public TokenDTO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}