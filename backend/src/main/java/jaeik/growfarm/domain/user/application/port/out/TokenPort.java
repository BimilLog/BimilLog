package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;

import java.util.Optional;

/**
 * <h2>Token Persistence Port</h2>
 * <p>토큰 정보 영속성 처리를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface TokenPort {

    /**
     * <h3>토큰 ID로 조회</h3>
     * <p>주어진 ID로 토큰 정보를 조회합니다.</p>
     *
     * @param id 토큰 ID
     * @return Optional<Token> 조회된 토큰 객체
     * @author Jaeik
     * @since  2.0.0
     */
    Optional<Token> findById(Long id);

    /**
     * <h3>사용자별 토큰 조회</h3>
     * <p>주어진 사용자에 대한 토큰 정보를 조회합니다.</p>
     *
     * @param user 사용자 엔티티
     * @return Optional<Token> 조회된 토큰 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Token> findByUsers(User user);

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     *
     * @param token 저장할 토큰 엔티티
     * @return Token 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Token save(Token token);
}