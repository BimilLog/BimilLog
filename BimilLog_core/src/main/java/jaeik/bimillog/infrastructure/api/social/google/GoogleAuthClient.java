package jaeik.bimillog.infrastructure.api.social.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <h2>구글 OAuth 인증 클라이언트</h2>
 * <p>구글 토큰 발급/갱신/폐기 엔드포인트를 호출합니다.</p>
 */
@FeignClient(name = "google-auth", url = "https://oauth2.googleapis.com")
public interface GoogleAuthClient {

    /**
     * <h3>인가 코드로 토큰 발급</h3>
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    GoogleTokenResponse requestToken(@RequestBody MultiValueMap<String, String> params);

    /**
     * <h3>리프레시 토큰으로 토큰 갱신</h3>
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    GoogleTokenResponse refreshToken(@RequestBody MultiValueMap<String, String> params);

    /**
     * <h3>토큰 폐기</h3>
     */
    @PostMapping(value = "/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void revokeToken(@RequestBody MultiValueMap<String, String> params);
}
