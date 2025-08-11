package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.entity.user.Users;

import java.util.Optional;

/**
 * <h2>User Persistence Port</h2>
 * <p>사용자 정보 영속성 처리를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 1.0
 */
public interface UserPort {
    Optional<Users> findById(Long id);
    Optional<Users> findByProviderAndSocialId(SocialProvider provider, String socialId);
    boolean existsByUserName(String userName);
    Users findByUserName(String userName);
    void deleteById(Long id);
    Setting save(Setting setting);
    Users save(Users user);
}
