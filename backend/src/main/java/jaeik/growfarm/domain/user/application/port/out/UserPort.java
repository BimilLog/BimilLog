package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>User Persistence Port</h2>
 * <p>사용자 정보 영속성 처리를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 1.0
 */
public interface UserPort {
    Optional<User> findById(Long id);
    Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId);
    boolean existsByUserName(String userName);
    User findByUserName(String userName);
    void deleteById(Long id);
    Setting save(Setting setting);
    User save(User user);
    
    // Setting 조회 기능 추가
    Optional<Setting> findSettingById(Long settingId);
}
