package jaeik.bimillog.infrastructure.adapter.auth.out.social;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.global.vo.KakaoKeyVO;
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
 * <p>
 * Strategy 패턴을 적용한 카카오 소셜 로그인 처리 전략 구현체입니다.
 * </p>
 * <p>
 * SocialLoginStrategy 인터페이스를 구현하여 카카오 API와의 통신을 담당합니다.
 * WebClient를 사용한 비동기 HTTP 통신으로 카카오 OAuth 2.0 플로우를 처리하고, 인증 코드로 토큰을 발급받아 사용자 정보를 조회합니다.
 * </p>
 * <p>
 * 이 전략이 존재하는 이유: 다양한 소셜 로그인 제공자(카카오, 네이버 등)에 대한 확장 가능한 아키텍처를 위해
 * Strategy 패턴을 적용하여 각 제공자별 세부 처리 로직을 캁싐화하고 새로운 제공자 추가 시 유연하게 대응하기 위해서입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class KakaoLoginStrategy implements SocialLoginStrategy {

    private final KakaoKeyVO kakaoKeyVO;
    private final WebClient webClient;

    public KakaoLoginStrategy(KakaoKeyVO kakaoKeyVO, WebClient.Builder webClientBuilder) {
        this.kakaoKeyVO = kakaoKeyVO;
        this.webClient = webClientBuilder.build();
    }

    /**
     * <h3>카카오 소셜 로그인 전체 처리</h3>
     * <p>카카오 OAuth 2.0 인증 코드를 받아 토큰 발급부터 사용자 정보 조회까지 전체 로그인 플로우를 처리합니다.</p>
     * <p>소셜 로그인 요청 시 카카오 인증 서버로부터 받은 인증 코드를 처리하기 위해 소셜 로그인 플로우에서 호출합니다.</p>
     * <p>내부적으로 getToken()과 getUserInfo() 메서드를 순차적으로 호출하여 리액티브 스트림 체인으로 처리합니다.</p>
     *
     * @param code 카카오 OAuth 2.0 인증 코드
     * @return Mono<StrategyLoginResult> 로그인 결과 (비동기 스트림)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Mono<StrategyLoginResult> login(String code) {
        return getToken(code)
                .flatMap(token -> getUserInfo(token.getAccessToken())
                        .map(userProfile -> new StrategyLoginResult(userProfile, token)));
    }

    /**
     * <h3>카카오 계정 연결 해제</h3>
     * <p>카카오 관리자 API를 사용하여 특정 사용자의 카카오 계정 연결을 완전히 해제합니다.</p>
     * <p>회원 탈퇴 처리 시 소셜 계정과의 연결을 완전히 끊기 위해 회원 탈퇴 플로우에서 호출합니다.</p>
     * <p>카카오 관리자 키(Admin Key)를 사용하여 서버 측에서 강제로 연결을 해제하므로 사용자가 다시 로그인하려면 새로 인증 과정을 거쳐야 합니다.</p>
     *
     * @param socialId 연결 해제할 카카오 사용자 ID
     * @return Mono<Void> 연결 해제 결과 (비동기 스트림)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Mono<Void> unlink(String socialId) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("target_id_type", "user_id");
        formData.add("target_id", socialId);

        return webClient.post()
                .uri(kakaoKeyVO.getUNLINK_URL())
                .header("Authorization", "KakaoAK " + kakaoKeyVO.getADMIN_KEY())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Kakao unlink failed: " + errorBody)))
                .bodyToMono(Void.class);
    }

    /**
     * <h3>카카오 로그아웃 처리</h3>
     * <p>사용자의 카카오 액세스 토큰을 사용하여 카카오 서버에서 로그아웃 처리를 수행합니다.</p>
     * <p>사용자 로그아웃 시 카카오 세션도 종료시켜 완전한 로그아웃을 위해 로그아웃 플로우에서 호출합니다.</p>
     * <p>비동기 방식으로 처리되며, 카카오 서버 오류 시에도 로그아웃 플로우를 방해하지 않도록 subscribe()로 안전하게 처리합니다.</p>
     *
     * @param accessToken 사용자의 카카오 액세스 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void logout(String accessToken) {
        webClient.post()
                .uri(kakaoKeyVO.getLOGOUT_URL())
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Kakao logout failed: " + errorBody)))
                .bodyToMono(Void.class)
                .subscribe();
    }

    /**
     * <h3>카카오 액세스 토큰 발급</h3>
     * <p>카카오 OAuth 2.0 인증 코드를 사용하여 카카오 인증 서버로부터 액세스 토큰과 리프레시 토큰을 발급받습니다.</p>
     * <p>카카오 소셜 로그인 처리 내부에서 사용자 정보 조회를 위한 선행 단계로 
login() 메서드에서 내부적으로 호출합니다.</p>
     * <p>Authorization Code Grant 플로우를 사용하여 카카오 인증 서버에 token exchange 요청을 전송합니다.</p>
     *
     * @param code 카카오 OAuth 2.0 인증 코드
     * @return Mono<Token> 도메인 Token 엔티티 (비동기 스트림)
     * @author Jaeik
     * @since 2.0.0
     */
    private Mono<Token> getToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoKeyVO.getCLIENT_ID());
        formData.add("client_secret", kakaoKeyVO.getCLIENT_SECRET());
        formData.add("redirect_uri", kakaoKeyVO.getREDIRECT_URI());
        formData.add("code", code);

        return webClient.post()
                .uri(kakaoKeyVO.getTOKEN_URL())
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Kakao token request failed: " + errorBody)))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(responseBody -> Token.createTemporaryToken(
                        (String) responseBody.get("access_token"),
                        (String) responseBody.get("refresh_token")
                ));
    }



    /**
     * <h3>카카오 사용자 정보 조회</h3>
     * <p>내부적으로 발급받은 액세스 토큰으로 카카오 사용자 정보 API에서 프로필 데이터를 조회합니다.</p>
     * <p>카카오 소셜 로그인 처리 내부에서 토큰 발급 후 사용자 정보를 획득하기 위해 login() 메서드에서 내부적으로 호출합니다.</p>
     * <p>카카오 API 응답에서 필요한 사용자 정보를 추출하여 도메인 SocialUserProfile 로 변환합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @return Mono<LoginResult.SocialUserProfile> 도메인 소셜 사용자 프로필 (비동기 스트림)
     * @author Jaeik
     * @since 2.0.0
     */
    private Mono<LoginResult.SocialUserProfile> getUserInfo(String accessToken) {
        return webClient.get()
                .uri(kakaoKeyVO.getUSER_INFO_URL())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Kakao user info request failed: " + errorBody)))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(responseBody -> {
                    Map<String, Object> kakaoAccount = (Map<String, Object>) responseBody.get("kakao_account");
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

                    String socialId = String.valueOf(responseBody.get("id"));
                    String nickname = (String) profile.get("nickname");
                    String thumbnailImage = (String) profile.get("thumbnail_image_url");

                    return new LoginResult.SocialUserProfile(
                            socialId,
                            null, // 카카오는 이메일을 제공하지 않음
                            getProvider(),
                            nickname,
                            thumbnailImage
                    );
                });
    }

    /**
     * <h3>소셜 로그인 제공자 식별자 반환</h3>
     * <p>현재 전략 구현체가 처리하는 소셜 로그인 제공자 타입을 반환합니다.</p>
     * <p>Strategy 패턴 구현에서 각 전략을 구별하고 적절한 전략 구현체를 선택하기 위해 SocialAdapter에서 호출합니다.</p>
     * <p>이 메서드를 통해 반환되는 값으로 SocialAdapter는 Map<SocialProvider, SocialLoginStrategy>에서 적절한 전략을 매핑합니다.</p>
     *
     * @return SocialProvider.KAKAO 카카오 소셜 로그인 제공자 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }
}
