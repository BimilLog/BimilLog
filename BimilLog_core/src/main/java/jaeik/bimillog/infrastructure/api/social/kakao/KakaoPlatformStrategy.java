package jaeik.bimillog.infrastructure.api.social.kakao;

import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import org.springframework.stereotype.Component;

/**
 * <h2>카카오 플랫폼 전략</h2>
 * <p>카카오 인증 전략을 하나의 플랫폼 전략으로 묶어 제공합니다.</p>
 */
@Component
public class KakaoPlatformStrategy extends SocialPlatformStrategy {

    /**
     * <h3>카카오 플랫폼 전략 생성자</h3>
     * <p>카카오 인증 전략을 통합하여 플랫폼 전략을 구성합니다.</p>
     *
     * @param kakaoAuthStrategy 카카오 인증 전략 구현체
     */
    public KakaoPlatformStrategy(KakaoAuthStrategy kakaoAuthStrategy) {
        super(SocialProvider.KAKAO, kakaoAuthStrategy);
    }
}
