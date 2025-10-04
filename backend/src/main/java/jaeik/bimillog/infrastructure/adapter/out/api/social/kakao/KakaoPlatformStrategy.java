package jaeik.bimillog.infrastructure.adapter.out.api.social.kakao;

import jaeik.bimillog.domain.global.application.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import org.springframework.stereotype.Component;

/**
 * <h2>카카오 플랫폼 전략</h2>
 * <p>카카오 인증/친구 전략을 하나의 플랫폼 전략으로 묶어 제공합니다.</p>
 */
@Component
public class KakaoPlatformStrategy extends SocialPlatformStrategy {

    public KakaoPlatformStrategy(KakaoAuthStrategy kakaoAuthStrategy,
                                 KakaoFriendStrategy kakaoFriendStrategy) {
        super(SocialProvider.KAKAO, kakaoAuthStrategy, kakaoFriendStrategy);
    }
}
