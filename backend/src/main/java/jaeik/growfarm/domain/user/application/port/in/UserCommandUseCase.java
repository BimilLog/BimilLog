package jaeik.growfarm.domain.user.application.port.in;

import jaeik.growfarm.domain.user.entity.SettingVO;
import jaeik.growfarm.domain.user.entity.User;

/**
 * <h2>사용자 명령 유스케이스</h2>
 * <p>사용자 관련 명령 요청을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserCommandUseCase {

    /**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 설정을 수정하는 메서드</p>
     *
     * @param userId    사용자 ID
     * @param settingVO 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    void updateUserSettings(Long userId, SettingVO settingVO);

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경하는 메서드</p>
     *
     * @param userId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @since 2.0.0
     * @author Jaeik
     */
    void updateUserName(Long userId, String newUserName);

    /**
     * <h3>사용자 삭제</h3>
     * <p>ID를 통해 사용자를 삭제하는 메서드</p>
     *
     * @param userId 삭제할 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteById(Long userId);

    /**
     * <h3>사용자 저장</h3>
     * <p>사용자 정보를 저장하거나 업데이트하는 메서드</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    User save(User user);
}
