package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.domain.user.entity.User;

import java.util.Optional;

public interface LoadUserPort {

    Optional<User> findByUserName(String userName);
    boolean existsByUserName(String userName);
}