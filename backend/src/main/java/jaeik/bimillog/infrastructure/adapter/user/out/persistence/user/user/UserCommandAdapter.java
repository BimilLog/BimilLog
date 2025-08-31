package jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user;

import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.entity.BlackList;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.blacklist.BlackListRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.setting.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>User Command Adapter</h2>
 * <p>사용자 정보 생성/수정/삭제를 위한 Persistence Adapter</p>
 * <p>CQRS 패턴에 따라 명령 전용 어댑터로 분리</p>
 * <p>SaveBlacklistPort 기능도 포함</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserCommandAdapter implements UserCommandPort {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final BlackListRepository blackListRepository;

    /**
     * <h3>사용자 정보 저장</h3>
     * <p>사용자 정보를 저장하거나 업데이트합니다.</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * <h3>설정 정보 저장</h3>
     * <p>설정 정보를 저장하거나 업데이트합니다.</p>
     *
     * @param setting 저장할 설정 엔티티
     * @return Setting 저장된 설정 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Setting save(Setting setting) {
        return settingRepository.save(setting);
    }

    /**
     * <h3>ID로 사용자 삭제</h3>
     * <p>주어진 ID의 사용자 정보를 삭제합니다.</p>
     *
     * @param id 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * <h3>블랙리스트 저장</h3>
     * <p>블랙리스트에 사용자 정보를 저장합니다.</p>
     *
     * @param blackList 저장할 블랙리스트 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(BlackList blackList) {
        blackListRepository.save(blackList);
    }
}