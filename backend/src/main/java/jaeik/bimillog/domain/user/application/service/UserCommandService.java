package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.user.web.UserCommandController;
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

    @Override
    public void deleteUser(Long userId) {

    }

    /**
     * <h3>사용자 역할을 BAN으로 변경</h3>
     * <p>사용자의 역할을 BAN으로 변경하여 일정 기간 서비스 이용을 제한합니다.</p>
     * <p>JWT 토큰 무효화는 JwtBlacklistEventListener가 이벤트를 통해 처리합니다.</p>
     *
     * @param userId 제재할 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void banUser(Long userId) {
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateRole(UserRole.BAN);

        log.info("사용자 제재 완료 - userId: {}, userName: {}, 역할 변경: BAN",
                userId, user.getUserName());
    }
}
