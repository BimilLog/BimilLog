package jaeik.bimillog.domain.user.application.port.in;

import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.auth.SaveUserAdapter;
import jaeik.bimillog.infrastructure.adapter.in.user.web.UserCommandController;

/**
 * <h2>사용자 명령 유스케이스</h2>
 * <p>사용자 관련 명령 요청을 처리하는 입력 포트입니다.</p>
 * <p>사용자 설정 수정, 닉네임 변경</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserCommandUseCase {

    /**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 알림 설정을 수정합니다.</p>
     * <p>{@link UserCommandController}에서 설정 수정 API 요청 시 호출됩니다.</p>
     *
     * @param userId    사용자 ID
     * @param setting 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    void updateUserSettings(Long userId, Setting setting);

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경합니다.</p>
     * <p>{@link UserCommandController}에서 닉네임 변경 API 요청 시 호출됩니다.</p>
     *
     * @param userId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @since 2.0.0
     * @author Jaeik
     */
    void updateUserName(Long userId, String newUserName);

    /**
     * <h3>사용자 엔티티 저장</h3>
     * <p>새로운 사용자를 저장하거나 기존 사용자를 업데이트합니다.</p>
     * <p>소셜 로그인 신규 회원가입 시 사용자 엔티티를 저장하는데 사용됩니다.</p>
     * <p>{@link SaveUserAdapter}에서 소셜 로그인 후 신규 사용자 저장 시 호출됩니다.</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    User saveUser(User user);
}
