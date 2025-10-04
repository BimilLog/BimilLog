package jaeik.bimillog.domain.global.application.strategy;

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

    public SocialProvider getSupportedProvider() {
        return provider;
    }

    public SocialAuthStrategy auth() {
        return authStrategy;
    }

    public Optional<SocialFriendStrategy> friend() {
        return Optional.ofNullable(friendStrategy);
    }
}
