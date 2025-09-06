package jaeik.bimillog.infrastructure.adapter.notification.out.persistence.user;

import jaeik.bimillog.domain.notification.application.port.out.NotificationToUserPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>사용자 어댑터</h2>
 * <p>Notification 도메인에서 User 도메인의 In-Port를 통해 접근하는 어댑터</p>
 * <p>헥사고날 아키텍처를 준수하여 UseCase를 통한 도메인간 통신</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationToUserAdapter implements NotificationToUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>사용자 ID로 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @return User 조회된 사용자 객체
     * @throws UserCustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User findById(Long userId) {
        return userQueryUseCase.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));
    }
}