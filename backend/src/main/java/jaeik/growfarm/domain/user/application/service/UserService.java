package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.SaveBlacklistPort;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.domain.BlackList;
import jaeik.growfarm.domain.user.domain.Setting;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserQueryUseCase, UserCommandUseCase {

    private final UserPort userPort;
    private final ApplicationEventPublisher eventPublisher;
    private final SaveBlacklistPort saveBlacklistPort;

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

    @Async
    @Transactional
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        log.info("User (Social ID: {}, Provider: {}) banned event received. Adding to blacklist.",
                event.getSocialId(), event.getProvider());
        BlackList blackList = BlackList.createBlackList(event.getSocialId(), event.getProvider());
        saveBlacklistPort.save(blackList);
    }
}
