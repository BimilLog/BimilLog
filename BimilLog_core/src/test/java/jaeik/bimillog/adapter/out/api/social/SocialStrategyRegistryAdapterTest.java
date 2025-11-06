package jaeik.bimillog.adapter.out.api.social;

import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.out.global.GlobalSocialStrategyAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * <h2>GlobalSocialStrategyAdapter 테스트</h2>
 * <p>소셜 플랫폼 전략 레지스트리의 핵심 동작을 검증합니다.</p>
 */
@DisplayName("GlobalSocialStrategyAdapter 단위 테스트")
@Tag("unit")
class SocialStrategyRegistryAdapterTest extends BaseUnitTest {

    @Mock
    private SocialPlatformStrategy kakaoStrategy;

    @Mock
    private SocialPlatformStrategy googleStrategy;

    @Mock
    private SocialPlatformStrategy naverStrategy;

    @Test
    @DisplayName("전략 자동 등록 - 여러 전략이 정상적으로 등록된다")
    void shouldRegisterMultipleStrategiesSuccessfully() {
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);
        given(googleStrategy.getSupportedProvider()).willReturn(SocialProvider.GOOGLE);
        given(naverStrategy.getSupportedProvider()).willReturn(SocialProvider.NAVER);

        List<SocialPlatformStrategy> strategies = Arrays.asList(kakaoStrategy, googleStrategy, naverStrategy);
        GlobalSocialStrategyAdapter registry = new GlobalSocialStrategyAdapter(strategies);

        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(kakaoStrategy);
        assertThat(registry.getStrategy(SocialProvider.GOOGLE)).isEqualTo(googleStrategy);
        assertThat(registry.getStrategy(SocialProvider.NAVER)).isEqualTo(naverStrategy);
    }

    @Test
    @DisplayName("중복 전략 처리 - 동일한 Provider를 가진 중복 전략은 첫 번째 전략만 등록된다")
    void shouldKeepFirstStrategyWhenDuplicateProviderFound() {
        SocialPlatformStrategy firstKakaoStrategy = kakaoStrategy;
        SocialPlatformStrategy duplicateKakaoStrategy = naverStrategy;

        given(firstKakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);
        given(duplicateKakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);

        List<SocialPlatformStrategy> strategies = Arrays.asList(firstKakaoStrategy, duplicateKakaoStrategy);
        GlobalSocialStrategyAdapter registry = new GlobalSocialStrategyAdapter(strategies);

        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(firstKakaoStrategy);
        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isNotEqualTo(duplicateKakaoStrategy);
    }

    @Test
    @DisplayName("지원하지 않는 Provider - 등록되지 않은 Provider 요청 시 예외 발생")
    void shouldThrowExceptionWhenRequestingUnsupportedProvider() {
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);

        List<SocialPlatformStrategy> strategies = Collections.singletonList(kakaoStrategy);
        GlobalSocialStrategyAdapter registry = new GlobalSocialStrategyAdapter(strategies);

        assertThatThrownBy(() -> registry.getStrategy(SocialProvider.GOOGLE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("지원하지 않는 소셜 제공자: GOOGLE")
            .hasMessageContaining("지원 제공자: [KAKAO]");
    }

    @Test
    @DisplayName("빈 전략 목록 - 전략이 없을 때 모든 Provider 요청에 예외 발생")
    void shouldThrowExceptionForAnyProviderWhenNoStrategiesRegistered() {
        GlobalSocialStrategyAdapter registry = new GlobalSocialStrategyAdapter(Collections.emptyList());

        for (SocialProvider provider : SocialProvider.values()) {
            assertThatThrownBy(() -> registry.getStrategy(provider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 소셜 제공자: " + provider)
                .hasMessageContaining("지원 제공자: []");
        }
    }

    @Test
    @DisplayName("단일 전략 등록 - 하나의 전략만 등록해도 정상 동작")
    void shouldWorkWithSingleStrategy() {
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);

        GlobalSocialStrategyAdapter registry = new GlobalSocialStrategyAdapter(Collections.singletonList(kakaoStrategy));

        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(kakaoStrategy);
        assertThatThrownBy(() -> registry.getStrategy(SocialProvider.GOOGLE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.getStrategy(SocialProvider.NAVER)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("전략 교체 불가 - 한 번 등록된 전략은 변경되지 않음을 검증")
    void shouldNotReplaceOnceRegisteredStrategy() {
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);
        GlobalSocialStrategyAdapter registry = new GlobalSocialStrategyAdapter(Collections.singletonList(kakaoStrategy));

        SocialPlatformStrategy firstStrategy = registry.getStrategy(SocialProvider.KAKAO);
        SocialPlatformStrategy secondStrategy = registry.getStrategy(SocialProvider.KAKAO);

        assertThat(firstStrategy).isSameAs(secondStrategy);
    }
}
