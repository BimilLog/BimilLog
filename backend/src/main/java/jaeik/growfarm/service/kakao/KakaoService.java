package jaeik.growfarm.service.kakao;

import jaeik.growfarm.dto.kakao.KakaoCheckConsentDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.global.auth.KakaoKeyVO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/*
 * 카카오 API 서비스
 * 카카오 API와의 통신을 처리하는 서비스 클래스
 * 수정일 : 2025-05-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {
    private final KakaoKeyVO kakaoKeyVO;
    private final WebClient.Builder webClientBuilder;

    /**
     * <h3>카카오 토큰 발급</h3>
     *
     * <p>
     * 카카오 인가 코드를 통해 액세스 토큰과 리프레시 토큰을 발급받는다.
     * </p>
     *
     * @param code 카카오 인가 코드
     * @return 토큰 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public TokenDTO getToken(String code) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoKeyVO.getCLIENT_ID());
        formData.add("redirect_uri", kakaoKeyVO.getREDIRECT_URI());
        formData.add("code", code);
        formData.add("client_secret", kakaoKeyVO.getCLIENT_SECRET());

        Mono<TokenDTO> response = webClient.post()
                .uri(kakaoKeyVO.getTOKEN_URL())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new CustomException(
                                        (HttpStatus) clientResponse.statusCode(), "카카오 토큰 발급 실패: " + errorBody))))
                .bodyToMono(TokenDTO.class);

        return response.block();
    }

    /**
     * <h3>카카오 로그아웃</h3>
     *
     * <p>
     * 카카오 액세스 토큰을 사용하여 카카오 서비스에서 로그아웃한다.
     * </p>
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @author Jaeik
     * @since 1.0.0
     */
    public void logout(String kakaoAccessToken) {
        WebClient webClient = webClientBuilder.build();

        Mono<String> response = webClient.post()
                .uri(kakaoKeyVO.getLOGOUT_URL())
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono.error(
                                new RuntimeException("로그아웃이 실패 했습니다.: " + clientResponse.statusCode())))
                .bodyToMono(String.class);

        String result = response.block();
    }

    // 카카오계정과 함께 로그아웃
    public String logoutWithKakao(String kakaoAccessToken) {
        WebClient webClient = webClientBuilder.build();

        Mono<String> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(kakaoKeyVO.getLOGOUT_WITH_KAKAO_URL())
                        .queryParam("client_id", kakaoKeyVO.getCLIENT_ID())
                        .queryParam("logout_redirect_uri",
                                kakaoKeyVO.getLOGOUT_WITH_REDIRECT_URL())
                        .build())
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("카카오계정과 함께 로그아웃이 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(String.class);

        return response.block();
    }

    // 연결 끊기
    public String unlink(String kakaoAccessToken) {
        WebClient webClient = webClientBuilder.build();

        Mono<String> response = webClient.post()
                .uri(kakaoKeyVO.getUNLINK_URL())
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("연결 끊기가 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(String.class);

        return response.block();
    }

    // 강제 연결 끊기
    public String unlinkByAdmin(Long kakaoId) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("target_id_type", "user_id");
        formData.add("target_id", kakaoId.toString());

        Mono<String> response = webClient.post()
                .uri(kakaoKeyVO.getUNLINK_URL())
                .header("Authorization", "KakaoAK " + kakaoKeyVO.getADMIN_KEY())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("강제 연결 끊기가 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(String.class);

        return response.block();
    }

    // 토큰 정보 보기
    public TokenDTO getTokenInfo(String kakaoAccessToken) {
        WebClient webClient = webClientBuilder.build();

        Mono<TokenDTO> response = webClient.get()
                .uri(kakaoKeyVO.getTOKEN_INFO_URL())
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("토큰 정보 조회가 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(TokenDTO.class);

        return response.block();
    }

    // 토큰 갱신하기
    public TokenDTO refreshToken(String refreshToken) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", kakaoKeyVO.getCLIENT_ID());
        formData.add("refresh_token", refreshToken);
        formData.add("client_secret", kakaoKeyVO.getCLIENT_SECRET());

        Mono<TokenDTO> response = webClient.post()
                .uri(kakaoKeyVO.getREFRESH_TOKEN_URL())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("토큰 갱신이 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(TokenDTO.class);

        return response.block();
    }

    // 사용자 정보 가져오기
    public KakaoInfoDTO getUserInfo(String kakaoAccessToken) {
        WebClient webClient = webClientBuilder.build();

        Mono<Map<String, Object>> response = webClient.get()
                .uri(kakaoKeyVO.getUSER_INFO_URL())
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });

        Map<String, Object> responseMap = response.block();

        if (responseMap == null) {
            throw new CustomException(ErrorCode.KAKAO_GET_USERINFO_FAIL);
        }

        KakaoInfoDTO kakaoInfoDTO = new KakaoInfoDTO();
        kakaoInfoDTO.setKakaoId(Long.parseLong(responseMap.get("id").toString()));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) responseMap.get("properties");
        if (properties != null) {
            kakaoInfoDTO.setKakaoNickname(properties.get("nickname").toString());
            kakaoInfoDTO.setThumbnailImage(properties.get("thumbnail_image").toString());
        }

        return kakaoInfoDTO;
    }

    // 친구 목록 가져오기
    public KakaoFriendListDTO getFriendList(String kakaoAccessToken, int offset) {
        WebClient webClient = webClientBuilder.build();

        Mono<KakaoFriendListDTO> response = webClient.get()
                .uri(kakaoKeyVO.getGET_FRIEND_LIST_URL() + "?offset=" + offset + "&limit=" + 20)
                .header("Authorization", "Bearer " + kakaoAccessToken.trim())
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("친구 목록 가져오기가 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(KakaoFriendListDTO.class);

        return response.block();
    }

    // 동의 내역 확인하기
    public KakaoCheckConsentDTO checkConsent(String kakaoAccessToken) {
        WebClient webClient = webClientBuilder.build();

        String scopesJson = "[\"friends\"]"; // JSON 배열 문자열로 직접 설정

        Mono<KakaoCheckConsentDTO> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("kapi.kakao.com")
                        .path("/v2/user/scopes")
                        .queryParam("scopes", scopesJson)
                        .build())
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("카카오 동의 조회 실패 응답: {}", body);
                                    return Mono.error(new RuntimeException(
                                            "동의 내역 확인 실패: " + body));
                                }))
                .bodyToMono(KakaoCheckConsentDTO.class);

        return response.block();
    }

    // 여러 사용자 정보 가져오기
    public String getMultipleUserInfo(Long[] targetIds) {
        WebClient webClient = webClientBuilder.build();

        Mono<String> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(kakaoKeyVO.getMULTIPLE_USER_INFO_URL())
                        .queryParam("target_ids", String.join(",",
                                java.util.Arrays.stream(targetIds)
                                        .map(String::valueOf)
                                        .toArray(String[]::new)))
                        .queryParam("target_id_type", "user_id")
                        .build())
                .header("Authorization", kakaoKeyVO.getADMIN_KEY())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("여러 사용자 정보 가져오기가 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(String.class);

        return response.block();
    }

    // 사용자 목록 가져오기
    public String getUserList(Integer limit, Long from_id, String order) {
        WebClient webClient = webClientBuilder.build();

        Mono<String> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(kakaoKeyVO.getUSER_LIST_URL())
                        .queryParam("limit", limit)
                        .queryParam("from_id", from_id)
                        .queryParam("order", order)
                        .build())
                .header("Authorization", kakaoKeyVO.getADMIN_KEY())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("사용자 목록 가져오기가 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(String.class);

        return response.block();
    }

    // 사용자 정보 저장하기
    public String saveUserInfo(String properties) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("properties", properties);

        Mono<String> response = webClient.post()
                .uri(kakaoKeyVO.getSAVE_USER_INFO_URL())
                .header("Authorization", kakaoKeyVO.getADMIN_KEY())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("사용자 정보 저장이 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(String.class);

        return response.block();
    }

    // 동의 철회하기
    public String revokeConsent(String kakaoAccessToken, String[] scopes) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("scopes", String.join(",", scopes));

        Mono<String> response = webClient.post()
                .uri(kakaoKeyVO.getREVOKE_CONSENT_URL())
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono
                                .error(new RuntimeException("동의 철회가 실패 했습니다.: "
                                        + clientResponse.statusCode())))
                .bodyToMono(String.class);

        return response.block();
    }
}
