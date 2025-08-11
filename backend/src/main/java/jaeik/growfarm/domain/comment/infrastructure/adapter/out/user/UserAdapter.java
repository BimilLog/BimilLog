package jaeik.growfarm.domain.comment.infrastructure.adapter.out.user;

import jaeik.growfarm.domain.comment.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPort {

    private final UserQueryUseCase userQueryUseCase;

    @Override
    public Optional<User> findById(Long userId) {
        return userQueryUseCase.findById(userId);
    }
}
