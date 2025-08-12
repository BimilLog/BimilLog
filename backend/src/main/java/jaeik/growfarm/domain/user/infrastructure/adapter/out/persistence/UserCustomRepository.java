package jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.user.ClientDTO;

import java.util.List;
import java.util.Optional;

public interface UserCustomRepository {

    Optional<User> findByIdWithSetting(Long id);

    ClientDTO findClientInfoById(Long id);

    List<String> findUserNamesInOrder(List<String> ids);
}
