package jaeik.bimillog.infrastructure.adapter.user.out.social;

import jaeik.bimillog.infrastructure.adapter.user.dto.KakaoFriendsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <h2>카카오 API Feign Client</h2>
 * <p>카카오 OAuth 및 API 호출을 담당하는 Feign Client입니다.</p>
 * <p>토큰 발급, 사용자 정보 조회, 로그아웃, 계정 연결 해제, 친구 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@FeignClient(name = "kakao-api", url = "https://kapi.kakao.com")
public interface KakaoApiClient {



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