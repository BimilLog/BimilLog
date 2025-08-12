package jaeik.growfarm.domain.user.application.port.in;

import jaeik.growfarm.dto.user.SettingDTO;

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
     * @param settingDTO 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    void updateUserSettings(Long userId, SettingDTO settingDTO);

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
}
