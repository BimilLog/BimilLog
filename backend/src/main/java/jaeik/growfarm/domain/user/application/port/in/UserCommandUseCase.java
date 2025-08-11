package jaeik.growfarm.domain.user.application.port.in;

import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.SettingDTO;

/**
 * <h2>User Command UseCase</h2>
 * <p>사용자 정보 생성, 수정, 삭제를 위한 In-Port</p>
 *
 * @author Jaeik
 * @version 1.0
 */
public interface UserCommandUseCase {
    void withdrawUser(Long userId);
    void updateUserSettings(Long userId, SettingDTO settingDTO);
    void updateUserName(Long userId, String newUserName);
}
