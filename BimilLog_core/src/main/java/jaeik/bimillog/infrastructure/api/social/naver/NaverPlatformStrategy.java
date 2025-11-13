package jaeik.bimillog.infrastructure.api.social.naver;

import jaeik.bimillog.domain.global.strategy.SocialPlatformStrategy;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import org.springframework.stereotype.Component;

/**
 * <h2>네이버 플랫폼 전략</h2>
 * <p>네이버 인증 전략을 하나의 플랫폼 전략으로 제공합니다.</p>
 * <p>네이버는 친구 API를 제공하지 않으므로 인증 전략만 등록됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class NaverPlatformStrategy extends SocialPlatformStrategy {

    /**
     * <h3>네이버 플랫폼 전략 생성자</h3>
     * <p>네이버 인증 전략을 통합하여 플랫폼 전략을 구성합니다.</p>
     * <p>Spring 컨테이너에 의해 자동으로 주입되며, 전략 레지스트리에 등록됩니다.</p>
     * <p>네이버는 친구 API를 제공하지 않으므로 friendStrategy는 null입니다.</p>
     *
     * @param naverAuthStrategy 네이버 인증 전략 구현체
     * @author Jaeik
     * @since 2.0.0
     */
    public NaverPlatformStrategy(NaverAuthStrategy naverAuthStrategy) {
        super(SocialProvider.NAVER, naverAuthStrategy);
    }
}
