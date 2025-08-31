package jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user;

import jaeik.bimillog.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserCustomRepository {

    /**
     * <h3>ID와 설정을 포함한 사용자 조회</h3>
     * <p>주어진 ID로 사용자 정보를 조회하며, 연관된 설정 정보도 함께 가져옵니다.</p>
     *
     * @param id 사용자 ID
     * @return Optional<User> 조회된 사용자 객체 (설정 정보 포함). 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findByIdWithSetting(Long id);


    /**
     * <h3>주어진 순서대로 사용자 이름 조회</h3>
     * <p>주어진 ID 목록에 해당하는 사용자 이름들을 요청된 순서대로 조회합니다.</p>
     *
     * @param ids 조회할 사용자 ID 문자열 리스트
     * @return List<String> 조회된 사용자 이름 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<String> findUserNamesInOrder(List<String> ids);
}
