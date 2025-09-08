package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 서비스</h2>
 * <p>사용자 관련 명령 유스케이스를 구현하는 애플리케이션 서비스</p>
 * <p>헥사고날 아키텍처에서 비즈니스 로직을 담당하며, 사용자 설정 업데이트 및 이벤트 처리 기능을 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserCommandService implements UserCommandUseCase {

    private final UserQueryPort userQueryPort;

    /**
     * <h3>사용자 설정 수정</h3>
     * <p>사용자의 설정을 수정하는 메서드</p>
     *
     * @param userId    사용자 ID
     * @param newSetting 수정할 설정 정보
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void updateUserSettings(Long userId, Setting newSetting) {
        if (newSetting == null) {
            throw new UserCustomException(UserErrorCode.INVALID_INPUT_VALUE);
        }
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
     * <p>사용자의 닉네임을 변경하는 메서드</p>
     * <p>Race Condition 방지를 위해 데이터베이스 UNIQUE 제약조건 위반 예외를 처리합니다.</p>
     *
     * @param userId      사용자 ID
     * @param newUserName 새로운 닉네임
     * @throws UserCustomException EXISTED_NICKNAME - 닉네임이 중복된 경우
     * @throws UserCustomException USER_NOT_FOUND - 사용자를 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
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


}
