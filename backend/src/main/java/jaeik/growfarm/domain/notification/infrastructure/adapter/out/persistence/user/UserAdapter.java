package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.user;

import jaeik.growfarm.domain.notification.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 어댑터</h2>
 * <p>Notification 도메인에서 User 도메인의 In-Port를 통해 접근하는 어댑터</p>
 * <p>헥사고날 아키텍처를 준수하여 UseCase를 통한 도메인간 통신</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component("notificationUserAdapter")
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>사용자 ID로 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long userId) {
        return userQueryUseCase.findById(userId);
    }
}