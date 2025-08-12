package jaeik.growfarm.domain.auth.infrastructure.adapter.out.strategy;

import jaeik.growfarm.dto.auth.KakaoFriendsResponse;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.infrastructure.auth.KakaoKeyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * <h2>카카오 로그인 전략</h2>
 * <p>카카오 소셜 로그인을 처리하는 클래스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class KakaoLoginStrategy implements SocialLoginStrategy {

    private final KakaoKeyVO kakaoKeyVO;
    private final WebClient.Builder webClientBuilder;

    /**
     * <h3>카카오 로그인 처리</h3>
     * <p>카카오 소셜 로그인 코드를 받아 사용자 정보를 조회하고 로그인 결과를 반환합니다.</p>
     *
     * @param code 카카오 소셜 로그인 코드
     * @return 로그인 결과 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public LoginResultDTO login(String code) {
        TokenDTO tokenDTO = getToken(code);
        SocialLoginUserData userData = getUserInfo(tokenDTO.accessToken());
        return LoginResultDTO.builder()
                .userData(userData)
                .tokenDTO(tokenDTO)
                .build();
    }

    /**
     * <h3>카카오 계정 연결 해제</h3>
     * <p>주어진 소셜 ID에 해당하는 카카오 계정의 연결을 해제합니다.</p>
     *
     * @param socialId 카카오 소셜 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void unlink(String socialId) {
        WebClient webClient = webClientBuilder.build();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("target_id_type", "user_id");
        formData.add("target_id", socialId);

        webClient.post()
                .uri(kakaoKeyVO.getUNLINK_URL())
                .header("Authorization", "KakaoAK " + kakaoKeyVO.getADMIN_KEY())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    /**
     * <h3>카카오 로그아웃</h3>
     * <p>주어진 액세스 토큰으로 카카오 로그아웃을 수행합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void logout(String accessToken) {
        WebClient webClient = webClientBuilder.build();
        webClient.post()
                .uri(kakaoKeyVO.getLOGOUT_URL())
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    /**
     * <h3>카카오 토큰 요청</h3>
     * <p>카카오 로그인 코드를 사용하여 액세스 토큰과 리프레시 토큰을 요청합니다.</p>
     *
     * @param code 카카오 로그인 코드
     * @return TokenDTO 액세스 토큰과 리프레시 토큰을 포함하는 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    private TokenDTO getToken(String code) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoKeyVO.getCLIENT_ID());
        formData.add("redirect_uri", kakaoKeyVO.getREDIRECT_URI());
        formData.add("code", code);

        Mono<Map<String, Object>> response = webClient.post()
                .uri(kakaoKeyVO.getTOKEN_URL())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});

        Map<String, Object> responseBody = response.block();
        return TokenDTO.builder()
                .accessToken((String) responseBody.get("access_token"))
                .refreshToken((String) responseBody.get("refresh_token"))
                .build();
    }

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>카카오 액세스 토큰을 사용하여 친구 목록을 조회합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param offset      조회 시작 위치 (기본값: 0)
     * @param limit       조회할 친구 수 (기본값: 10, 최대: 100)
     * @return KakaoFriendsResponse 친구 목록 응답 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    public KakaoFriendsResponse getFriendList(String accessToken, Integer offset, Integer limit) {
        WebClient webClient = webClientBuilder.build();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(kakaoKeyVO.getGET_FRIEND_LIST_URL())
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new CustomException(ErrorCode.KAKAO_API_ERROR, new RuntimeException(errorBody)))))
                .bodyToMono(KakaoFriendsResponse.class)
                .block();
    }

    /**
     * <h3>카카오 사용자 정보 조회</h3>
     * <p>카카오 액세스 토큰을 사용하여 사용자 정보를 조회합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @return SocialLoginUserData 사용자 정보 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    private SocialLoginUserData getUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.build();

        Mono<Map<String, Object>> response = webClient.get()
                .uri(kakaoKeyVO.getUSER_INFO_URL())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});

        Map<String, Object> responseBody = response.block();
        Map<String, Object> kakaoAccount = (Map<String, Object>) responseBody.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String socialId = String.valueOf(responseBody.get("id"));
        String nickname = (String) profile.get("nickname");
        String thumbnailImage = (String) profile.get("thumbnail_image_url");

        return SocialLoginUserData.builder()
                .provider(getProvider())
                .socialId(socialId)
                .nickname(nickname)
                .profileImageUrl(thumbnailImage)
                .build();
    }

    /**
     * <h3>소셜 제공자 정보 조회</h3>
     * <p>현재 소셜 로그인 전략의 제공자를 반환합니다.</p>
     *
     * @return SocialProvider 현재 소셜 로그인 전략의 제공자
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }
}
