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

    /**
     * <h3>카카오 플랫폼 전략 생성자</h3>
     * <p>카카오 인증 전략과 친구 전략을 통합하여 플랫폼 전략을 구성합니다.</p>
     * <p>Spring 컨테이너에 의해 자동으로 주입되며, 전략 레지스트리에 등록됩니다.</p>
     *
     * @param kakaoAuthStrategy 카카오 인증 전략 구현체
     * @param kakaoFriendStrategy 카카오 친구 전략 구현체
     * @author Jaeik
     * @since 2.0.0
     */
    public KakaoPlatformStrategy(KakaoAuthStrategy kakaoAuthStrategy,
                                 KakaoFriendStrategy kakaoFriendStrategy) {
        super(SocialProvider.KAKAO, kakaoAuthStrategy, kakaoFriendStrategy);
    }
}
