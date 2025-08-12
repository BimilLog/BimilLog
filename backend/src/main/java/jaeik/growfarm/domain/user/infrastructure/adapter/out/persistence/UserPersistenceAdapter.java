package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.post.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.user.application.port.out.SaveBlacklistPort;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.entity.BlackList;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.global.domain.SocialProvider;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>User Persistence Adapter</h2>
 * <p>사용자 정보 영속성 관리를 위한 Outgoing-Adapter</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPort, SaveBlacklistPort, LoadUserPort, jaeik.growfarm.domain.paper.application.port.out.LoadUserPort {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final BlackListRepository blacklistRepository;

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
    public Optional<User> findByUserName(String userName) {
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

    @Override
    public Optional<Setting> findSettingById(Long settingId) {
        return settingRepository.findById(settingId);
    }
}
