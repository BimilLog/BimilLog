package jaeik.growfarm.domain.user.application.port.in;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import java.util.Optional;

/**
 * <h2>User Query UseCase</h2>
 * <p>사용자 정보 조회를 위한 In-Port</p>
 *
 * @author Jaeik
 * @version 1.0
 */
public interface UserQueryUseCase {
    Optional<User> findById(Long userId);
    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);
    boolean existsByUserName(String userName);
    User findByUserName(String userName);
}
