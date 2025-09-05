package jaeik.bimillog.infrastructure.adapter.user.out.social;

import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.KakaoFriendsDTO;
import jaeik.bimillog.infrastructure.auth.KakaoKeyVO;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * <h2>카카오 소셜 어댑터</h2>
 * <p>카카오의 소셜 기능(친구 목록 조회 등)을 담당하는 어댑터</p>
 * <p>인증(로그인) 기능과 분리하여 카카오의 부가 기능만 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class KakaoSocialAdapter implements SocialAdapter {

    private final KakaoKeyVO kakaoKeyVO;
    private final WebClient webClient;

    public KakaoSocialAdapter(KakaoKeyVO kakaoKeyVO, WebClient.Builder webClientBuilder) {
        this.kakaoKeyVO = kakaoKeyVO;
        this.webClient = webClientBuilder.build();
    }

    /**
     * <h3>소셜 제공자 반환</h3>
     * <p>카카오 제공자를 반환합니다.</p>
     *
     * @return SocialProvider.KAKAO
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    /**
     * <h3>카카오 친구 목록 조회</h3>
     * <p>카카오 API를 통해 사용자의 친구 목록을 조회합니다.</p>
     *
     * @param accessToken 카카오 액세스 토큰
     * @param offset      조회 시작 위치 (기본값: 0)
     * @param limit       조회할 친구 수 (기본값: 10, 최대: 100)
     * @return Mono<KakaoFriendsDTO> 친구 목록 응답 (비동기)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Mono<KakaoFriendsDTO> getFriendList(String accessToken, Integer offset, Integer limit) {
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
                .bodyToMono(KakaoFriendsDTO.class)
                .timeout(java.time.Duration.ofSeconds(10));
    }
}