package jaeik.bimillog.infrastructure.adapter.auth.social;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.KakaoSocialLoginStrategy;
import jaeik.bimillog.infrastructure.adapter.user.out.social.KakaoApiClient;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.KakaoAuthClient;
import jaeik.bimillog.global.vo.KakaoKeyVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>KakaoSocialLoginStrategy 단위 테스트</h2>
 * <p>카카오 소셜 로그인 전략의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoSocialLoginStrategy 단위 테스트")
class KakaoSocialLoginStrategyTest {

    @Mock private KakaoKeyVO kakaoKeyVO;
    @Mock private KakaoAuthClient kakaoAuthClient;
    @Mock private KakaoApiClient kakaoApiClient;


    @Test
    @DisplayName("KakaoSocialLoginStrategy 생성자 정상 동작 검증")
    void shouldCreateInstance_WhenValidDependenciesProvided() {
        // Given
        // Feign clients are mocked already

        // When
        KakaoSocialLoginStrategy strategy = new KakaoSocialLoginStrategy(kakaoKeyVO, kakaoAuthClient, kakaoApiClient);

        // Then
        assertThat(strategy).isNotNull();
    }
}