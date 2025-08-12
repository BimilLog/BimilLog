package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.domain.auth.infrastructure.adapter.out.strategy.SocialLoginStrategy;
import jaeik.growfarm.global.domain.SocialProvider;
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

    /**
     * <h3>소셜 로그인</h3>
     * <p>소셜 로그인 요청을 처리하고, 로그인 결과를 반환</p>
     *
     * @param provider 소셜 제공자 (예: KAKAO, NAVER 등)
     * @param code     소셜 로그인 인증 코드
     * @return 로그인 결과 DTO
     * @since 2.0.0
     * @author Jaeik
     */
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