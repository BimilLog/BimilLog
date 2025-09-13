package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserCommandPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.user.in.web.UserCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 서비스</h2>
 * <p>UserCommandUseCase의 구현체로 사용자 정보 수정 로직을 담당합니다.</p>
 * <p>사용자 알림 설정 수정, 닉네임 변경</p>
 * <p>Race Condition 방지, 데이터베이스 제약조건 예외 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommandService implements UserCommandUseCase {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;

    /**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 알림 설정을 수정합니다.</p>
     * <p>{@link UserCommandController}에서 설정 수정 API 요청 시 호출됩니다.</p>
     *
     * @param userId    사용자 ID
     * @param newSetting 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void updateUserSettings(Long userId, Setting newSetting) {
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateSettings(
                newSetting.isMessageNotification(),
                newSetting.isCommentNotification(),
                newSetting.isPostFeaturedNotification()
        );
        // JPA 변경 감지로 자동 저장
    }

    /**
     * <h3>닉네임 변경</h3>
     * <p>사용자의 닉네임을 변경합니다.</p>
     * <p>Race Condition 방지를 위해 데이터베이스 UNIQUE 제약조건 위반 예외를 처리합니다.</p>
     * <p>{@link UserCommandController}에서 닉네임 변경 API 요청 시 호출됩니다.</p>
     *
     * @param userId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @throws UserCustomException EXISTED_NICKNAME - 닉네임이 중복된 경우
     * @throws UserCustomException USER_NOT_FOUND - 사용자를 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void updateUserName(Long userId, String newUserName) {
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        try {
            // 엔티티가 비즈니스 로직 처리. JPA 변경 감지로 자동 저장되므로 명시적 save는 불필요합니다.
            user.changeUserName(newUserName, userQueryPort);
        } catch (DataIntegrityViolationException e) {
            // Race Condition 발생 시: 다른 사용자가 동시에 같은 닉네임으로 변경한 경우
            log.warn("닉네임 경쟁 상태 감지됨 - 사용자 ID: {}, 새 닉네임: {}", userId, newUserName, e);
            throw new UserCustomException(UserErrorCode.EXISTED_NICKNAME);
        }
    }

    /**
     * <h3>사용자 엔티티 저장</h3>
     * <p>새로운 사용자를 저장하거나 기존 사용자를 업데이트합니다.</p>
     * <p>소셜 로그인 신규 회원가입 시 사용자 엔티티를 저장하는데 사용됩니다.</p>
     * <p>{@link jaeik.bimillog.infrastructure.adapter.auth.out.auth.SaveUserAdapter}에서 소셜 로그인 후 신규 사용자 저장 시 호출됩니다.</p>
     *
     * @param user 저장할 사용자 엔티티
     * @return User 저장된 사용자 엔티티
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public User saveUser(User user) {
        return userCommandPort.save(user);
    }

}
