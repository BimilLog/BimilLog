package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 JPA 어댑터</h2>
 * <p>사용자 정보 조회를 위한 JPA 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthUserJpaAdapter implements LoadUserPort {

    private final UserQueryUseCase userQueryUseCase;

    @Override
    public Optional<Users> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userQueryUseCase.findByProviderAndSocialId(provider, socialId);
    }

    @Override
    public Optional<Users> findById(Long userId) {
        return userQueryUseCase.findById(userId);
    }
}