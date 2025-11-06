package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.auth.service.SocialLoginService;
import jaeik.bimillog.domain.global.application.port.out.GlobalSocialStrategyPort;
import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>소셜 로그인 전략 레지스트리 구현체</h2>
 * <p>소셜 제공자별 로그인 전략을 중앙에서 관리하는 레지스트리입니다.</p>
 * <p>전략 자동 등록, 동적 전략 선택, EnumMap 기반 효율적 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@Slf4j
public class GlobalSocialStrategyAdapter implements GlobalSocialStrategyPort {

    private final Map<SocialProvider, SocialPlatformStrategy> strategies;

    /**
     * <h3>전략 레지스트리 생성자</h3>
     * <p>Spring 컨테이너에 등록된 모든 전략 구현체를 자동으로 수집하여 등록합니다.</p>
     * <p>각 전략의 getSupportedProvider() 메서드를 통해 제공자를 식별하고 매핑합니다.</p>
     *
     * @param strategyList Spring 컨테이너에 등록된 모든 SocialPlatformStrategy 구현체
     * @author Jaeik
     * @since 2.0.0
     */
    public GlobalSocialStrategyAdapter(List<SocialPlatformStrategy> strategyList) {
        this.strategies = new EnumMap<>(SocialProvider.class);
        for (SocialPlatformStrategy strategy : strategyList) {
            SocialProvider provider = strategy.getSupportedProvider();
            strategies.putIfAbsent(provider, strategy);
        }
    }

    /**
     * <h3>제공자별 전략 조회</h3>
     * <p>소셜 제공자에 해당하는 로그인 전략 구현체를 반환합니다.</p>
     * <p>{@link SocialLoginService}에서 소셜 로그인 처리 시 전략 선택을 위해 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO, GOOGLE, NAVER 등)
     * @return 해당 제공자의 로그인 전략 구현체
     * @throws IllegalArgumentException 지원하지 않는 제공자인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SocialPlatformStrategy getStrategy(SocialProvider provider) {
        return Optional.ofNullable(strategies.get(provider))
            .orElseThrow(() -> new IllegalArgumentException(
                "지원하지 않는 소셜 제공자: " + provider +
                ". 지원 제공자: " + strategies.keySet()
            ));
    }
}