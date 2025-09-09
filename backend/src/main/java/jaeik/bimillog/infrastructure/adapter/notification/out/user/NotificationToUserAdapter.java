package jaeik.bimillog.infrastructure.adapter.notification.out.user;

import jaeik.bimillog.domain.notification.application.port.out.NotificationToUserPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>알림-사용자 도메인 연결 어댑터</h2>
 * <p>
 * Notification 도메인에서 User 도메인으로 접근하는 아웃바운드 어댑터입니다.
 * NotificationToUserPort를 구현하여 크로스 도메인 통신을 담당합니다.
 * </p>
 * <p>헥사고날 아키텍처 원칙에 따라 User 도메인의 UseCase를 통해 사용자 정보를 조회합니다.</p>
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