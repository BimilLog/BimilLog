package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.LoadTokenPort;
import jaeik.growfarm.domain.user.domain.Token;
import jaeik.growfarm.repository.token.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>토큰 JPA 어댑터</h2>
 * <p>토큰 정보 조회를 위한 JPA 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class TokenJpaAdapter implements LoadTokenPort {

    private final TokenRepository tokenRepository;

    @Override
    public Optional<Token> findById(Long tokenId) {
        return tokenRepository.findById(tokenId);
    }
}