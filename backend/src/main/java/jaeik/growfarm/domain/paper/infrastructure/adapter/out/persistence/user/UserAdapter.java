package jaeik.growfarm.domain.paper.infrastructure.adapter.out.persistence.user;

import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>사용자 조회 어댑터</h2>
 * <p>Paper 도메인에서 User 도메인의 In-Port를 통해 접근하는 어댑터</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPort {

    private final UserQueryUseCase userQueryUseCase;

    @Override
    public Optional<User> findByUserName(String userName) {
        return userQueryUseCase.findByUserName(userName);
    }

    @Override
    public boolean existsByUserName(String userName) {
        return userQueryUseCase.existsByUserName(userName);
    }
}