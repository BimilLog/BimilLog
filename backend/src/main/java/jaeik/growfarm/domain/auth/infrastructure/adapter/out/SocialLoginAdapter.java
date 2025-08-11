package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.service.auth.SocialLoginManager;
import jaeik.growfarm.service.auth.strategy.SocialLoginStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>소셜 로그인 어댑터</h2>
 * <p>소셜 로그인 처리를 위한 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class SocialLoginAdapter implements SocialLoginPort {

    private final SocialLoginManager socialLoginManager;

    @Override
    public SocialLoginStrategy.LoginResult login(SocialProvider provider, String code) {
        return socialLoginManager.login(provider, code);
    }

    @Override
    public void unlink(SocialProvider provider, String socialId) {
        socialLoginManager.unlink(provider, socialId);
    }

    @Override
    public void logout(SocialProvider provider, String accessToken) {
        socialLoginManager.logout(provider, accessToken);
    }
}