package jaeik.growfarm.domain.auth.application.port.out;

import jaeik.growfarm.domain.user.domain.Token;

import java.util.Optional;

/**
 * <h2>토큰 조회 포트</h2>
 * <p>토큰 정보 조회를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadTokenPort {

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     *
     * @param tokenId 토큰 ID
     * @return 토큰 정보 (없으면 Optional.empty())
     */
    Optional<Token> findById(Long tokenId);
}