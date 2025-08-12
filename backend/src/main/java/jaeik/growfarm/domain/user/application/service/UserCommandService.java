
package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.event.UserSignedUpEvent;
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
public class UserCommandService implements UserCommandUseCase {

    private final UserPort userPort;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    public void updateUserSettings(Long userId, SettingDTO settingDTO) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Setting setting = user.getSetting();
        setting.updateSetting(settingDTO);
        userPort.save(user);
    }

    @Override
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
    public void handleUserSignedUpEvent(UserSignedUpEvent event) {
        User user = userPort.findById(event.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        Setting setting = Setting.createSetting();
        user.updateSetting(setting);
        userPort.save(user);
        log.info("Initialized default settings for new user (ID: {})", event.getUserId());
    }
}
