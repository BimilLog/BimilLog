package jaeik.bimillog.infrastructure.adapter.auth.social;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.KakaoLoginStrategy;
import jaeik.bimillog.global.vo.KakaoKeyVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * <h2>KakaoLoginStrategy 단위 테스트</h2>
 * <p>카카오 로그인 전략의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoLoginStrategy 단위 테스트")
class KakaoLoginStrategyTest {

    @Mock private KakaoKeyVO kakaoKeyVO;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;

    @Test
    @DisplayName("소셜 제공자 확인 - 카카오 Provider 반환")
    void shouldReturnKakaoProvider() {
        // Given
        given(webClientBuilder.build()).willReturn(webClient);
        KakaoLoginStrategy kakaoLoginStrategy = new KakaoLoginStrategy(kakaoKeyVO, webClientBuilder);

        // When
        SocialProvider provider = kakaoLoginStrategy.getProvider();

        // Then
        assertThat(provider).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("KakaoLoginStrategy 생성자 정상 동작 검증")
    void shouldCreateInstance_WhenValidDependenciesProvided() {
        // Given
        given(webClientBuilder.build()).willReturn(webClient);

        // When
        KakaoLoginStrategy strategy = new KakaoLoginStrategy(kakaoKeyVO, webClientBuilder);

        // Then
        assertThat(strategy).isNotNull();
        assertThat(strategy.getProvider()).isEqualTo(SocialProvider.KAKAO);
    }
}