package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.domain.user.domain.User;

public interface LoadUserPort {
    User findByUserName(String userName);
}