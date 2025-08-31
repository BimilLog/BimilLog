package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>사용자 조회 Port</h2>
 * <p>사용자 정보 조회 기능을 위한 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadUserPort {

    /**
     * <h3>사용자 이름으로 사용자 조회</h3>
     * <p>주어진 사용자 이름에 해당하는 사용자를 조회합니다.</p>
     *
     * @param userName 조회할 사용자의 이름
     * @return 사용자 엔티티 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findByUserName(String userName);

    /**
     * <h3>사용자 이름 존재 여부 확인</h3>
     * <p>주어진 사용자 이름이 존재하는지 여부를 확인합니다.</p>
     *
     * @param userName 확인할 사용자의 이름
     * @return 사용자 이름이 존재하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByUserName(String userName);
}