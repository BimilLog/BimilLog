package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.CheckBlacklistPort;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.repository.user.BlackListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>블랙리스트 JPA 어댑터</h2>
 * <p>블랙리스트 확인을 위한 JPA 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class BlacklistJpaAdapter implements CheckBlacklistPort {

    private final BlackListRepository blackListRepository;

    @Override
    public boolean existsByProviderAndSocialId(SocialProvider provider, String socialId) {
        return blackListRepository.existsByProviderAndSocialId(provider, socialId);
    }
}