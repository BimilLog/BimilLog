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
    Optional<Token> findById(Long id);
    Optional<Token> findByUser(User user);
    Token save(Token token);
}