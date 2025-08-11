package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.domain.Setting;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserQueryUseCase, UserCommandUseCase {

    private final UserPort userPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<User> findById(Long userId) {
        return userPort.findById(userId);
    }

    @Override
    public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userPort.findByProviderAndSocialId(provider, socialId);
    }

    @Override
    public boolean existsByUserName(String userName) {
        return userPort.existsByUserName(userName);
    }

    @Override
    public User findByUserName(String userName) {
        return userPort.findByUserName(userName);
    }

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.withdraw();
        eventPublisher.publishEvent(new UserWithdrawnEvent(this, userId));
        userPort.save(user);
    }

    @Override
    @Transactional
    public void updateUserSettings(Long userId, SettingDTO settingDTO) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Setting setting = user.getSetting();
        setting.updateSetting(settingDTO);
        userPort.save(user);
    }

    @Override
    @Transactional
    public void updateUserName(Long userId, String newUserName) {
        if (userPort.existsByUserName(newUserName)) {
            throw new CustomException(ErrorCode.EXISTED_NICKNAME);
        }
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateUserName(newUserName);
        userPort.save(user);
    }
}
