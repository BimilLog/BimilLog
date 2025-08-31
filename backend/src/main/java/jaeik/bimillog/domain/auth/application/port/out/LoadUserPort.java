package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>사용자 조회 포트</h2>
 * <p>Auth 도메인에서 User 도메인의 정보를 조회하기 위한 아웃바운드 포트</p>
 * <p>헥사고날 아키텍처 원칙에 따라 다른 도메인의 유스케이스 대신 Out 포트를 통해 접근</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadUserPort {

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<User> findById(Long id);
}