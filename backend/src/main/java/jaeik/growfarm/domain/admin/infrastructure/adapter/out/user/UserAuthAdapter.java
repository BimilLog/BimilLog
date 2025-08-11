package jaeik.growfarm.domain.admin.infrastructure.adapter.out.user;

import jaeik.growfarm.domain.admin.application.port.out.UserAuthPort;
import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuthAdapter implements UserAuthPort {

    private final UserCommandUseCase userCommandUseCase;

    @Override
    public void withdraw(Long userId) {
        userCommandUseCase.withdrawUser(userId);
    }
}
