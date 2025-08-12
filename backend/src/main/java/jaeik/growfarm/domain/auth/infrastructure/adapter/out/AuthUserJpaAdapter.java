package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
import jaeik.growfarm.domain.auth.application.port.out.LoadUserPort;
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

    private final UserRepository userRepository;
    /**
     * <h3>사용자 정보를 SocialProvider와 socialId로 조회</h3>
     *
     * @param provider SocialProvider
     * @param socialId 소셜 ID
     * @return Optional<User> 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userRepository.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>사용자 ID로 사용자 정보 조회</h3>
     *
     * @param userId 사용자 ID
     * @return Optional<User> 사용자 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
}