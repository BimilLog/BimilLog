package jaeik.bimillog.infrastructure.api.social.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * <h2>구글 사용자 정보 API 클라이언트</h2>
 */
@FeignClient(name = "google-userinfo", url = "https://www.googleapis.com")
public interface GoogleUserInfoClient {

    /**
     * <h3>사용자 정보 조회</h3>
     */
    @GetMapping("/oauth2/v3/userinfo")
    GoogleUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
