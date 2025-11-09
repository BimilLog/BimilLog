package jaeik.bimillog.domain.global.strategy;

import jaeik.bimillog.domain.member.entity.SocialProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * <h2>소셜 플랫폼 전략</h2>
 * <p>플랫폼별 인증/친구 전략 묶음을 캡슐화합니다.</p>
 */
public abstract class SocialPlatformStrategy {

    private final SocialProvider provider;
    private final SocialAuthStrategy authStrategy;
    private final SocialFriendStrategy friendStrategy;

    protected SocialPlatformStrategy(SocialProvider provider,
                                     SocialAuthStrategy authStrategy,
                                     SocialFriendStrategy friendStrategy) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.authStrategy = Objects.requireNonNull(authStrategy, "authStrategy must not be null");
        if (!authStrategy.getProvider().equals(provider)) {
            throw new IllegalArgumentException("인증 전략 제공자와 플랫폼 제공자가 일치하지 않습니다.");
        }
        if (friendStrategy != null && !friendStrategy.getProvider().equals(provider)) {
            throw new IllegalArgumentException("친구 전략 제공자와 플랫폼 제공자가 일치하지 않습니다.");
        }
        this.friendStrategy = friendStrategy;
    }

    protected SocialPlatformStrategy(SocialProvider provider,
                                     SocialAuthStrategy authStrategy) {
        this(provider, authStrategy, null);
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

    /**
     * <h3>친구 전략 조회</h3>
     * <p>이 플랫폼의 친구 관련 전략 구현체를 Optional로 반환합니다.</p>
     * <p>모든 소셜 플랫폼이 친구 API를 제공하는 것은 아니므로 Optional로 반환합니다.</p>
     *
     * @return 친구 전략 Optional (없을 수 있음)
     * @author Jaeik
     * @since 2.0.0
     */
    public Optional<SocialFriendStrategy> friend() {
        return Optional.ofNullable(friendStrategy);
    }
}
