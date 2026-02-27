package jaeik.bimillog.unit.domain.auth;

import jaeik.bimillog.domain.auth.adapter.SocialStrategyAdapter;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * <h2>SocialStrategyAdapter 테스트</h2>
 * <p>소셜 플랫폼 전략 레지스트리의 핵심 동작을 검증합니다.</p>
 */
@DisplayName("SocialStrategyAdapter 단위 테스트")
@Tag("unit")
class SocialStrategyAdapterTest extends BaseUnitTest {

    @Mock
    private SocialStrategy kakaoStrategy;

    @Mock
    private SocialStrategy googleStrategy;

    @Mock
    private SocialStrategy naverStrategy;

    @Test
    @DisplayName("전략 자동 등록 - 여러 전략이 정상적으로 등록된다")
    void shouldRegisterMultipleStrategiesSuccessfully() {
        given(kakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);
        given(googleStrategy.getProvider()).willReturn(SocialProvider.GOOGLE);
        given(naverStrategy.getProvider()).willReturn(SocialProvider.NAVER);

        List<SocialStrategy> strategies = Arrays.asList(kakaoStrategy, googleStrategy, naverStrategy);
        SocialStrategyAdapter registry = new SocialStrategyAdapter(strategies);

        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(kakaoStrategy);
        assertThat(registry.getStrategy(SocialProvider.GOOGLE)).isEqualTo(googleStrategy);
        assertThat(registry.getStrategy(SocialProvider.NAVER)).isEqualTo(naverStrategy);
    }

    @Test
    @DisplayName("중복 전략 처리 - 동일한 Provider를 가진 중복 전략은 첫 번째 전략만 등록된다")
    void shouldKeepFirstStrategyWhenDuplicateProviderFound() {
        SocialStrategy firstKakaoStrategy = kakaoStrategy;
        SocialStrategy duplicateKakaoStrategy = naverStrategy;

        given(firstKakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);
        given(duplicateKakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);

        List<SocialStrategy> strategies = Arrays.asList(firstKakaoStrategy, duplicateKakaoStrategy);
        SocialStrategyAdapter registry = new SocialStrategyAdapter(strategies);

        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(firstKakaoStrategy);
        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isNotEqualTo(duplicateKakaoStrategy);
    }

    @Test
    @DisplayName("지원하지 않는 Provider - 등록되지 않은 Provider 요청 시 null 반환")
    void shouldReturnNullWhenRequestingUnsupportedProvider() {
        given(kakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);

        List<SocialStrategy> strategies = Collections.singletonList(kakaoStrategy);
        SocialStrategyAdapter registry = new SocialStrategyAdapter(strategies);

        assertThat(registry.getStrategy(SocialProvider.GOOGLE)).isNull();
    }

    @Test
    @DisplayName("빈 전략 목록 - 전략이 없을 때 모든 Provider 요청에 null 반환")
    void shouldReturnNullForAnyProviderWhenNoStrategiesRegistered() {
        SocialStrategyAdapter registry = new SocialStrategyAdapter(Collections.emptyList());

        for (SocialProvider provider : SocialProvider.values()) {
            assertThat(registry.getStrategy(provider)).isNull();
        }
    }

    @Test
    @DisplayName("단일 전략 등록 - 하나의 전략만 등록해도 정상 동작")
    void shouldWorkWithSingleStrategy() {
        given(kakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);

        SocialStrategyAdapter registry = new SocialStrategyAdapter(Collections.singletonList(kakaoStrategy));

        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(kakaoStrategy);
        assertThat(registry.getStrategy(SocialProvider.GOOGLE)).isNull();
        assertThat(registry.getStrategy(SocialProvider.NAVER)).isNull();
    }

    @Test
    @DisplayName("전략 교체 불가 - 한 번 등록된 전략은 변경되지 않음을 검증")
    void shouldNotReplaceOnceRegisteredStrategy() {
        given(kakaoStrategy.getProvider()).willReturn(SocialProvider.KAKAO);
        SocialStrategyAdapter registry = new SocialStrategyAdapter(Collections.singletonList(kakaoStrategy));

        SocialStrategy firstStrategy = registry.getStrategy(SocialProvider.KAKAO);
        SocialStrategy secondStrategy = registry.getStrategy(SocialProvider.KAKAO);

        assertThat(firstStrategy).isSameAs(secondStrategy);
    }
}
