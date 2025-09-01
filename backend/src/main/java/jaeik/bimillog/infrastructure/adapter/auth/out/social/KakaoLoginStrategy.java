package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.TokenVO;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.bimillog.infrastructure.auth.KakaoKeyVO;
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
     * @return 로그인 결과 (비동기)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Mono<StrategyLoginResult> login(String code) {
        return getToken(code)
                .flatMap(tokenVO -> getUserInfo(tokenVO.accessToken())
                        .map(userData -> new StrategyLoginResult(userData, tokenVO)));
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
    public Mono<Void> unlink(String socialId) {
        WebClient webClient = webClientBuilder.build();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("target_id_type", "user_id");
        formData.add("target_id", socialId);

        return webClient.post()
                .uri(kakaoKeyVO.getUNLINK_URL())
                .header("Authorization", "KakaoAK " + kakaoKeyVO.getADMIN_KEY())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Void.class);
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
     * @return TokenValue 액세스 토큰과 리프레시 토큰을 포함하는 값 객체 (비동기)
     * @since 2.0.0
     * @author Jaeik
     */
    private Mono<TokenVO> getToken(String code) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoKeyVO.getCLIENT_ID());
        formData.add("redirect_uri", kakaoKeyVO.getREDIRECT_URI());
        formData.add("code", code);

        return webClient.post()
                .uri(kakaoKeyVO.getTOKEN_URL())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(responseBody -> TokenVO.builder()
                        .accessToken((String) responseBody.get("access_token"))
                        .refreshToken((String) responseBody.get("refresh_token"))
                        .build());
    }



    /**
     * <h3>카카오 사용자 정보 조회</h3>
     * <p>카카오 액세스 토큰을 사용하여 사용자 정보를 조회합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @return SocialLoginUserData 사용자 정보 DTO (비동기)
     * @since 2.0.0
     * @author Jaeik
     */
    private Mono<SocialLoginUserData> getUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.build();

        return webClient.get()
                .uri(kakaoKeyVO.getUSER_INFO_URL())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(responseBody -> {
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
                });
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
