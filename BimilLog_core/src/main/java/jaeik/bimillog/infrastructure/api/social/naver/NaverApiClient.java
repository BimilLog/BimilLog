package jaeik.bimillog.infrastructure.api.social.naver;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * <h2>네이버 API Feign Client</h2>
 * <p>네이버 사용자 정보 조회 API와 통신하는 Feign Client입니다.</p>
 * <p>프로필 조회 전용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@FeignClient(name = "naver-api", url = "https://openapi.naver.com")
public interface NaverApiClient {

    /**
     * <h3>네이버 사용자 프로필 조회</h3>
     * <p>액세스 토큰을 사용하여 네이버 사용자 정보를 조회합니다.</p>
     *
     * @param authorization 인증 헤더 (Bearer {access_token})
     * @return 사용자 정보 응답 (response 객체에 id, nickname, profile_image 등 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/v1/nid/me")
    Map<String, Object> getUserInfo(@RequestHeader("Authorization") String authorization);
}
