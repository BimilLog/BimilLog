package jaeik.bimillog.infrastructure.api.social.google;

import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import org.springframework.stereotype.Component;

/**
 * <h2>구글 플랫폼 전략</h2>
 * <p>구글 인증 전략을 플랫폼 전략으로 묶어 레지스트리에 등록합니다.</p>
 */
@Component
public class GooglePlatformStrategy extends SocialPlatformStrategy {

    public GooglePlatformStrategy(GoogleAuthStrategy googleAuthStrategy) {
        super(SocialProvider.GOOGLE, googleAuthStrategy);
    }
}
