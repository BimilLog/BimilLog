package jaeik.bimillog.infrastructure.api.social.naver;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * <h2>네이버 인증 Feign Client</h2>
 * <p>네이버 OAuth 인증 서버와 통신하는 Feign Client입니다.</p>
 * <p>토큰 발급/삭제 전용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@FeignClient(name = "naver-auth", url = "https://nid.naver.com")
public interface NaverAuthClient {

    /**
     * <h3>네이버 액세스 토큰 발급</h3>
     * <p>인증 코드를 사용하여 액세스 토큰과 리프레시 토큰을 발급받습니다.</p>
     *
     * @param params 토큰 요청 파라미터 (grant_type, client_id, client_secret, code, state)
     * @return 토큰 응답 (access_token, refresh_token, token_type, expires_in)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping(value = "/oauth2.0/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> getToken(@RequestParam Map<String, String> params);

    /**
     * <h3>네이버 액세스 토큰 삭제</h3>
     * <p>네이버 로그아웃 및 연결 해제 시 사용하는 토큰 삭제 API입니다.</p>
     * <p>grant_type은 'delete'로 설정합니다.</p>
     * <p>네이버는 토큰 삭제도 POST 메서드를 사용합니다 (grant_type으로 구분)</p>
     *
     * @param params 토큰 삭제 파라미터 (grant_type=delete, client_id, client_secret, access_token, service_provider=NAVER)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping(value = "/oauth2.0/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteToken(@RequestParam Map<String, String> params);

    /**
     * <h3>네이버 액세스 토큰 갱신</h3>
     * <p>리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.</p>
     * <p>grant_type은 'refresh_token'으로 설정합니다.</p>
     *
     * @param params 토큰 갱신 파라미터 (grant_type=refresh_token, client_id, client_secret, refresh_token)
     * @return 갱신된 토큰 (access_token, token_type, expires_in)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping(value = "/oauth2.0/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> refreshToken(@RequestParam Map<String, String> params);
}
