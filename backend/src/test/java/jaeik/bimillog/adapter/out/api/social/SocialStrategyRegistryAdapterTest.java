package jaeik.bimillog.adapter.out.api.social;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.out.api.social.SocialStrategyRegistryAdapter;
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
 * <h2>SocialStrategyRegistryAdapter 테스트</h2>
 * <p>소셜 로그인 전략 레지스트리의 핵심 동작을 검증하는 단위 테스트</p>
 * <p>전략 자동 등록, 중복 처리, 동적 선택 기능 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("SocialStrategyRegistryAdapter 단위 테스트")
@Tag("unit")
class SocialStrategyRegistryAdapterTest extends BaseUnitTest {

    @Mock
    private SocialStrategyPort kakaoStrategy;

    @Mock
    private SocialStrategyPort googleStrategy;

    @Mock
    private SocialStrategyPort naverStrategy;

    @Test
    @DisplayName("전략 자동 등록 - 여러 전략이 정상적으로 등록된다")
    void shouldRegisterMultipleStrategiesSuccessfully() {
        // Given - 각 전략이 지원하는 Provider 설정
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);
        given(googleStrategy.getSupportedProvider()).willReturn(SocialProvider.GOOGLE);
        given(naverStrategy.getSupportedProvider()).willReturn(SocialProvider.NAVER);

        List<SocialStrategyPort> strategies = Arrays.asList(
            kakaoStrategy, googleStrategy, naverStrategy
        );

        // When - 레지스트리 생성 시 전략들이 자동 등록됨
        SocialStrategyRegistryAdapter registry = new SocialStrategyRegistryAdapter(strategies);

        // Then - 각 Provider별로 올바른 전략이 반환되는지 검증
        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(kakaoStrategy);
        assertThat(registry.getStrategy(SocialProvider.GOOGLE)).isEqualTo(googleStrategy);
        assertThat(registry.getStrategy(SocialProvider.NAVER)).isEqualTo(naverStrategy);
    }

    @Test
    @DisplayName("중복 전략 처리 - 동일한 Provider를 가진 중복 전략은 첫 번째 전략만 등록된다")
    void shouldKeepFirstStrategyWhenDuplicateProviderFound() {
        // Given - 동일한 Provider를 반환하는 두 개의 전략
        SocialStrategyPort firstKakaoStrategy = kakaoStrategy;
        SocialStrategyPort duplicateKakaoStrategy = naverStrategy; // 실제로는 KAKAO를 반환하도록 설정

        given(firstKakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);
        given(duplicateKakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);

        List<SocialStrategyPort> strategies = Arrays.asList(
            firstKakaoStrategy, duplicateKakaoStrategy
        );

        // When - 중복된 전략을 포함한 레지스트리 생성
        SocialStrategyRegistryAdapter registry = new SocialStrategyRegistryAdapter(strategies);

        // Then - 첫 번째로 등록된 전략이 유지되는지 검증
        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(firstKakaoStrategy);
        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isNotEqualTo(duplicateKakaoStrategy);
    }

    @Test
    @DisplayName("지원하지 않는 Provider - 등록되지 않은 Provider 요청 시 예외 발생")
    void shouldThrowExceptionWhenRequestingUnsupportedProvider() {
        // Given - KAKAO 전략만 등록된 레지스트리
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);

        List<SocialStrategyPort> strategies = Collections.singletonList(kakaoStrategy);
        SocialStrategyRegistryAdapter registry = new SocialStrategyRegistryAdapter(strategies);

        // When & Then - 등록되지 않은 GOOGLE Provider 요청 시 예외 발생
        assertThatThrownBy(() -> registry.getStrategy(SocialProvider.GOOGLE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("지원하지 않는 소셜 제공자: GOOGLE")
            .hasMessageContaining("지원 제공자: [KAKAO]");
    }

    @Test
    @DisplayName("빈 전략 목록 - 전략이 없을 때 모든 Provider 요청에 예외 발생")
    void shouldThrowExceptionForAnyProviderWhenNoStrategiesRegistered() {
        // Given - 빈 전략 목록으로 레지스트리 생성
        List<SocialStrategyPort> emptyStrategies = Collections.emptyList();
        SocialStrategyRegistryAdapter registry = new SocialStrategyRegistryAdapter(emptyStrategies);

        // When & Then - 모든 Provider 요청에 대해 예외 발생
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
        // Given - KAKAO 전략만 있는 경우
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);

        List<SocialStrategyPort> strategies = Collections.singletonList(kakaoStrategy);
        SocialStrategyRegistryAdapter registry = new SocialStrategyRegistryAdapter(strategies);

        // When & Then - KAKAO는 정상 반환, 다른 Provider는 예외
        assertThat(registry.getStrategy(SocialProvider.KAKAO)).isEqualTo(kakaoStrategy);

        assertThatThrownBy(() -> registry.getStrategy(SocialProvider.GOOGLE))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> registry.getStrategy(SocialProvider.NAVER))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("전략 교체 불가 - 한 번 등록된 전략은 변경되지 않음을 검증")
    void shouldNotReplaceOnceRegisteredStrategy() {
        // Given - 초기 전략 등록
        given(kakaoStrategy.getSupportedProvider()).willReturn(SocialProvider.KAKAO);
        List<SocialStrategyPort> initialStrategies = Collections.singletonList(kakaoStrategy);
        SocialStrategyRegistryAdapter registry = new SocialStrategyRegistryAdapter(initialStrategies);

        // 첫 번째 전략 확인
        SocialStrategyPort firstStrategy = registry.getStrategy(SocialProvider.KAKAO);

        // When - 같은 레지스트리에서 다시 전략을 가져옴
        SocialStrategyPort secondStrategy = registry.getStrategy(SocialProvider.KAKAO);

        // Then - 동일한 인스턴스가 반환되는지 검증
        assertThat(firstStrategy).isSameAs(secondStrategy);
    }
}