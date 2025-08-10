package jaeik.growfarm.service.auth.strategy;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.kakao.KakaoInfoDTO;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.global.auth.KakaoKeyVO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoLoginStrategy implements SocialLoginStrategy {

    private final KakaoKeyVO kakaoKeyVO;
    private final WebClient.Builder webClientBuilder;

    @Override
    public LoginResult login(String code) {
        TokenDTO tokenDTO = getToken(code);
        SocialLoginUserData userData = getUserInfo(tokenDTO.getAccessToken());
        return new LoginResult(userData, tokenDTO);
    }

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

    private TokenDTO getToken(String code) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoKeyVO.getCLIENT_ID());
        formData.add("redirect_uri", kakaoKeyVO.getREDIRECT_URL());
        formData.add("code", code);

        Mono<Map<String, Object>> response = webClient.post()
                .uri(kakaoKeyVO.getTOKEN_URL())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> responseBody = response.block();
        return TokenDTO.builder()
                .accessToken((String) responseBody.get("access_token"))
                .refreshToken((String) responseBody.get("refresh_token"))
                .build();
    }

    private SocialLoginUserData getUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.build();

        Mono<Map<String, Object>> response = webClient.get()
                .uri(kakaoKeyVO.getUSER_INFO_URL())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class);

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


    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }
}
