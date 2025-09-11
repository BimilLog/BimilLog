package jaeik.bimillog.infrastructure.adapter.user.social;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.user.out.social.KakaoSocialAdapter;
import jaeik.bimillog.global.vo.KakaoKeyVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>KakaoSocialAdapter 테스트</h2>
 * <p>카카오 소셜 어댑터의 기본 동작 테스트</p>
 * <p>복잡한 WebClient 모킹보다는 기본 동작 검증에 집중</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class KakaoSocialAdapterTest {

    @Mock
    private KakaoKeyVO kakaoKeyVO;
    
    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private KakaoSocialAdapter kakaoSocialAdapter;

    @Test
    @DisplayName("정상 케이스 - 소셜 제공자 반환")
    void shouldReturnKakaoProvider_WhenGetProviderCalled() {
        // When: 소셜 제공자 조회 실행
        SocialProvider result = kakaoSocialAdapter.getProvider();

        // Then: 카카오 제공자가 반환되는지 검증
        assertThat(result).isEqualTo(SocialProvider.KAKAO);
    }



}