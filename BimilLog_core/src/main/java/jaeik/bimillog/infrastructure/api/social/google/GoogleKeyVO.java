package jaeik.bimillog.infrastructure.api.social.google;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <h2>구글 OAuth 키 VO</h2>
 * <p>구글 OAuth 연동에 필요한 키 값을 보관합니다.</p>
 */
@Getter
@Component
public class GoogleKeyVO {

    @Value("${spring.google.client-id}")
    private String CLIENT_ID;

    @Value("${spring.google.client-secret}")
    private String CLIENT_SECRET;

    @Value("${spring.google.redirect-uri}")
    private String REDIRECT_URI;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    public String getTokenUrl() {
        return TOKEN_URL;
    }

    public String getRevokeUrl() {
        return REVOKE_URL;
    }

    public String getUserInfoUrl() {
        return USER_INFO_URL;
    }
}
