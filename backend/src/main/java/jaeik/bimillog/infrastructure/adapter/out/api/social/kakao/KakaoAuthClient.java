package jaeik.bimillog.infrastructure.adapter.out.api.social.kakao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * <h2>카카오 인증 Feign Client</h2>
 * <p>카카오 OAuth 인증 서버와 통신하는 Feign Client입니다.</p>
 * <p>토큰 발급 전용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@FeignClient(name = "kakao-auth", url = "https://kauth.kakao.com")
public interface KakaoAuthClient {

    /**
     * <h3>카카오 액세스 토큰 발급</h3>
     * <p>인증 코드를 사용하여 액세스 토큰과 리프레시 토큰을 발급받습니다.</p>
     *
     * @param contentType Content-Type 헤더 (application/x-www-form-urlencoded;charset=utf-8)
     * @param params 토큰 요청 파라미터 (grant_type, client_id, client_secret, redirect_uri, code)
     * @return 토큰 응답 (access_token, refresh_token 등)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> getToken(@RequestHeader("Content-Type") String contentType,
                                 @RequestParam Map<String, String> params);
}