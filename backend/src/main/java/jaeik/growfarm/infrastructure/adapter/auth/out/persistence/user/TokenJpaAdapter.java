package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.auth.application.port.out.LoadTokenPort;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
import jaeik.growfarm.domain.user.entity.Token;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * <h2>토큰 JPA 어댑터</h2>
 * <p>토큰 정보 조회를 위한 JPA 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class TokenJpaAdapter implements LoadTokenPort {

    private final TokenRepository tokenRepository;

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     *
     * @param tokenId 토큰 ID
     * @return 토큰 정보 (없으면 Optional.empty())
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Optional<Token> findById(Long tokenId) {
        return tokenRepository.findById(tokenId);
    }
}