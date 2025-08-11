package jaeik.growfarm.domain.user.application.port.in;

import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.entity.user.Users;
import java.util.Optional;

/**
 * <h2>User Query UseCase</h2>
 * <p>사용자 정보 조회를 위한 In-Port</p>
 *
 * @author Jaeik
 * @version 1.0
 */
public interface UserQueryUseCase {
    Optional<Users> findById(Long userId);
    Optional<Users> findByProviderAndSocialId(SocialProvider provider, String socialId);
    boolean existsByUserName(String userName);
    Users findByUserName(String userName);
}
