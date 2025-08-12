package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.domain.user.entity.User;

public interface LoadUserPort {

    User findByUserName(String userName);
    boolean existsByUserName(String userName);
}