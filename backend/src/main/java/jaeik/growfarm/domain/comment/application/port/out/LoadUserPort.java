package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>Comment 도메인에서 User 도메인의 데이터를 조회하기 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadUserPort {

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findById(Long userId);
}