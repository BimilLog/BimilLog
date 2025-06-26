package jaeik.growfarm.service.user;

import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** * <h2>유저 업데이트 서비스</h2>
 * <p>
 * 유저의 DB작업을 처리하는 서비스
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserUpdateService {

    /**
     * <h3>유저 이름 업데이트</h3>
     *
     * <p>
     * 유저의 이름을 업데이트합니다.
     * </p>
     *
     * @param userName 업데이트할 유저 이름
     * @param user     업데이트할 유저 엔티티
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void userNameUpdate(String userName, Users user) {
        user.updateUserName(userName);
    }

    /**
     * <h3>설정 업데이트</h3>
     *
     * <p>
     * 유저의 설정을 업데이트합니다.
     * </p>
     *
     * @param settingDTO 업데이트할 설정 정보
     * @param setting    업데이트할 설정 엔티티
     */
    @Transactional
    public void settingUpdate(SettingDTO settingDTO, Setting setting) {
        setting.updateSetting(settingDTO);
    }
}
