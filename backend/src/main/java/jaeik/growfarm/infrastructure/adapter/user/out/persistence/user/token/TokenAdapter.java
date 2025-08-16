package jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token;

import jaeik.growfarm.domain.user.application.port.out.TokenPort;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>Token Persistence Adapter</h2>
 * <p>토큰 정보 영속성 관리를 위한 Outgoing-Adapter</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class TokenAdapter implements TokenPort {

    private final TokenRepository tokenRepository;

    @Override
    public Optional<Token> findById(Long id) {
        return tokenRepository.findById(id);
    }

    @Override
    public Optional<Token> findByUser(User user) {
        return tokenRepository.findByUser(user);
    }

    @Override
    public Token save(Token token) {
        return tokenRepository.save(token);
    }

}