package jaeik.bimillog.infrastructure.adapter.user.out.social;

import jaeik.bimillog.infrastructure.adapter.user.dto.KakaoFriendsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * <h2>카카오 API Feign Client</h2>
 * <p>카카오 OAuth 및 API 호출을 담당하는 Feign Client입니다.</p>
 * <p>사용자 정보 조회, 로그아웃, 계정 연결 해제, 친구 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@FeignClient(name = "kakao-api", url = "https://kapi.kakao.com")
public interface KakaoApiClient {

    /**
     * <h3>카카오 사용자 정보 조회</h3>
     * <p>액세스 토큰을 사용하여 카카오 사용자 정보를 조회합니다.</p>
     *
     * @param authorization Bearer 토큰 (형식: "Bearer {accessToken}")
     * @return 사용자 정보 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/v2/user/me")
    Map<String, Object> getUserInfo(@RequestHeader("Authorization") String authorization);

    /**
     * <h3>카카오 로그아웃</h3>
     * <p>사용자를 카카오에서 로그아웃 처리합니다.</p>
     *
     * @param authorization Bearer 토큰 (형식: "Bearer {accessToken}")
     * @param contentType Content-Type 헤더
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/v1/user/logout")
    void logout(@RequestHeader("Authorization") String authorization,
                @RequestHeader("Content-Type") String contentType);

    /**
     * <h3>카카오 계정 연결 해제</h3>
     * <p>카카오 관리자 키를 사용하여 사용자의 계정 연결을 해제합니다.</p>
     *
     * @param authorization KakaoAK 토큰 (형식: "KakaoAK {adminKey}")
     * @param params 연결 해제 파라미터 (target_id_type, target_id)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping(value = "/v1/user/unlink", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void unlink(@RequestHeader("Authorization") String authorization,
                @RequestParam Map<String, String> params);

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>사용자의 카카오 친구 목록을 조회합니다.</p>
     *
     * @param authorization Bearer 토큰 (형식: "Bearer {accessToken}")
     * @param offset 조회 시작 위치
     * @param limit 조회할 친구 수
     * @return 친구 목록 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/v1/api/talk/friends")
    KakaoFriendsDTO getFriends(@RequestHeader("Authorization") String authorization,
                               @RequestParam("offset") Integer offset,
                               @RequestParam("limit") Integer limit);
}