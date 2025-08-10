package jaeik.growfarm.service.auth;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.service.auth.strategy.SocialLoginStrategy;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SocialLoginManager {

    private final Map<SocialProvider, SocialLoginStrategy> strategies = new EnumMap<>(SocialProvider.class);

    public SocialLoginManager(List<SocialLoginStrategy> strategyList) {
        for (SocialLoginStrategy strategy : strategyList) {
            strategies.put(strategy.getProvider(), strategy);
        }
    }

    public SocialLoginStrategy.LoginResult login(SocialProvider provider, String code) {
        SocialLoginStrategy strategy = getStrategy(provider);
        return strategy.login(code);
    }

    public void unlink(SocialProvider provider, String socialId) {
        SocialLoginStrategy strategy = getStrategy(provider);
        strategy.unlink(socialId);
    }

    public void logout(SocialProvider provider, String accessToken) {
        SocialLoginStrategy strategy = getStrategy(provider);
        strategy.logout(accessToken);
    }

    private SocialLoginStrategy getStrategy(SocialProvider provider) {
        SocialLoginStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new CustomException(ErrorCode.INVALID_SOCIAL_PROVIDER);
        }
        return strategy;
    }
}

