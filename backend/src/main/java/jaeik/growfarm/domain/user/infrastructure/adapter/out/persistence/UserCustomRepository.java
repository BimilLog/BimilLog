package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserCustomRepository {

    Optional<User> findByIdWithSetting(Long id);


    List<String> findUserNamesInOrder(List<String> ids);
}
