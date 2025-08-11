package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.user.domain.User;

import java.util.Optional;

public interface LoadUserPort {

    Optional<User> findById(Long userId);
}
