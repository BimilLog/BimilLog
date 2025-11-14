package jaeik.bimillog.infrastructure.api.social.google;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <h2>구글 사용자 정보 응답</h2>
 * <p>구글 UserInfo 엔드포인트의 응답을 매핑합니다.</p>
 */
public record GoogleUserInfoResponse(
        String sub,
        String name,
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName,
        String picture,
        String email,
        @JsonProperty("email_verified") Boolean emailVerified
) {
}
