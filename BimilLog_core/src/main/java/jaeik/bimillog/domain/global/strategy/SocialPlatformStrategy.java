package jaeik.bimillog.domain.global.strategy;

import jaeik.bimillog.domain.member.entity.SocialProvider;

import java.util.Objects;

/**
 * <h2>소셜 플랫폼 전략</h2>
 * <p>플랫폼별 인증 전략 묶음을 캡슐화합니다.</p>
 */
public abstract class SocialPlatformStrategy {
    private final SocialProvider provider;
    private final SocialAuthStrategy authStrategy;

    protected SocialPlatformStrategy(SocialProvider provider, SocialAuthStrategy authStrategy) {
        this.provider = provider;
        this.authStrategy = authStrategy;
    }

    /**
     * <h3>지원하는 소셜 제공자 조회</h3>
     * <p>이 플랫폼 전략이 지원하는 소셜 제공자를 반환합니다.</p>
     *
     * @return 지원하는 소셜 제공자 (KAKAO 등)
     * @author Jaeik
     * @since 2.0.0
     */
    public SocialProvider getSupportedProvider() {
        return provider;
    }

    /**
     * <h3>인증 전략 조회</h3>
     * <p>이 플랫폼의 소셜 인증 전략 구현체를 반환합니다.</p>
     * <p>OAuth 인증, 로그아웃, 계정 연결 해제 등의 기능을 제공합니다.</p>
     *
     * @return 소셜 인증 전략
     * @author Jaeik
     * @since 2.0.0
     */
    public SocialAuthStrategy auth() {
        return authStrategy;
    }
}
