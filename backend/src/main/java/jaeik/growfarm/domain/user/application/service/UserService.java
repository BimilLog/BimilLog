package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.UserPort;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserQueryUseCase, UserCommandUseCase {

    private final UserPort userPort;

    @Override
    public Optional<Users> findById(Long userId) {
        return userPort.findById(userId);
    }

    @Override
    public Optional<Users> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userPort.findByProviderAndSocialId(provider, socialId);
    }

    @Override
    public boolean existsByUserName(String userName) {
        return userPort.existsByUserName(userName);
    }

    @Override
    public Users findByUserName(String userName) {
        return userPort.findByUserName(userName);
    }

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        // TO-DO: 다른 도메인(auth, comment, notification 등)과의 연동 로직 필요 (Event-driven으로 변경 예정)
        userPort.deleteById(userId);
    }

    @Override
    @Transactional
    public void updateUserSettings(Long userId, SettingDTO settingDTO) {
        Users user = userPort.findById(userId)
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
        Users user = userPort.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateUserName(newUserName);
        userPort.save(user);
    }
}
