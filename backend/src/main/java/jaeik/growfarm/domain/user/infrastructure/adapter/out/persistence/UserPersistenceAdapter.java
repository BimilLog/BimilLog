package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.user.application.port.out.SaveBlacklistPort;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.domain.BlackList;
import jaeik.growfarm.domain.user.domain.Setting;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.BlacklistRepository;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.SettingRepository;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>User Persistence Adapter</h2>
 * <p>사용자 정보 영속성 관리를 위한 Outgoing-Adapter</p>
 *
 * @author Jaeik
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPort, SaveBlacklistPort, jaeik.growfarm.domain.post.application.port.out.LoadUserPort {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final BlacklistRepository blacklistRepository;

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userRepository.findByProviderAndSocialId(provider, socialId);
    }

    @Override
    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Override
    public User findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Setting save(Setting setting) {
        return settingRepository.save(setting);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void save(BlackList blackList) {
        blacklistRepository.save(blackList);
    }

    @Override
    public User getReferenceById(Long userId) {
        return userRepository.getReferenceById(userId);
    }
}
