package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.infrastructure.adapter.out.strategy.SocialLoginStrategy;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.dto.auth.LoginResultDTO;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SocialLoginAdapter implements SocialLoginPort {

    private final Map<SocialProvider, SocialLoginStrategy> strategies = new EnumMap<>(SocialProvider.class);

    public SocialLoginAdapter(List<SocialLoginStrategy> strategyList) {
        strategyList.forEach(strategy -> strategies.put(strategy.getProvider(), strategy));
    }

    @Override
    public LoginResultDTO login(SocialProvider provider, String code) {
        SocialLoginStrategy strategy = strategies.get(provider);
        return strategy.login(code);
    }

    @Override
    public void unlink(SocialProvider provider, String socialId) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.unlink(socialId);
    }

    @Override
    public void logout(SocialProvider provider, String accessToken) {
        SocialLoginStrategy strategy = strategies.get(provider);
        strategy.logout(accessToken);
    }
}