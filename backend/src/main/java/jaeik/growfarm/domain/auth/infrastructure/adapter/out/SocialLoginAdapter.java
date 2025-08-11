package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.infrastructure.adapter.out.strategy.SocialLoginStrategy;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SocialLoginAdapter implements SocialLoginPort {

    private final Map<SocialProvider, SocialLoginStrategy> strategies = new EnumMap<>(SocialProvider.class);

    public SocialLoginAdapter(List<SocialLoginStrategy> strategyList) {
        for (SocialLoginStrategy strategy : strategyList) {
            strategies.put(strategy.getProvider(), strategy);
        }
    }

    @Override
    public LoginResultDTO login(SocialProvider provider, String code) {
        SocialLoginStrategy strategy = getStrategy(provider);
        return strategy.login(code);
    }

    @Override
    public void unlink(SocialProvider provider, String socialId) {
        SocialLoginStrategy strategy = getStrategy(provider);
        strategy.unlink(socialId);
    }

    @Override
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