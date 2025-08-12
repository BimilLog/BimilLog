package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.domain.user.application.port.out.SaveBlacklistPort;
import jaeik.growfarm.domain.user.entity.BlackList;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserCommandUseCase {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SaveBlacklistPort saveBlacklistPort;

    @Override
    public void updateUserSettings(Long userId, SettingDTO settingDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Setting setting = user.getSetting();
        setting.updateSetting(settingDTO);
        userRepository.save(user);
    }

    @Override
    public void updateUserName(Long userId, String newUserName) {
        if (userRepository.existsByUserName(newUserName)) {
            throw new CustomException(ErrorCode.EXISTED_NICKNAME);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateUserName(newUserName);
        userRepository.save(user);
    }

    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        log.info("User (Social ID: {}, Provider: {}) banned event received. Adding to blacklist.",
                event.getSocialId(), event.getProvider());
        BlackList blackList = BlackList.createBlackList(event.getSocialId(), event.getProvider());
        saveBlacklistPort.save(blackList);
    }
}
