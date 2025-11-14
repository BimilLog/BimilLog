package jaeik.bimillog.infrastructure.api.social.google;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <h2>구글 토큰 응답</h2>
 * <p>구글 OAuth 토큰 엔드포인트 응답을 매핑합니다.</p>
 */
public record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        String scope
) {
}
