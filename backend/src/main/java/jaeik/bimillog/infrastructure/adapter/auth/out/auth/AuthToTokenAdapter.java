package jaeik.bimillog.infrastructure.adapter.auth.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.AuthToTokenPort;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * <h2>인증 토큰 어댑터</h2>
 * <p>Auth 도메인에서 Token 엔티티에 접근하는 어댑터입니다.</p>
 * <p>토큰 ID로 토큰 조회, 사용자의 모든 토큰 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class AuthToTokenAdapter implements AuthToTokenPort {

    private final TokenRepository tokenRepository;

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     * <p>특정 토큰 ID에 해당하는 Token 엔티티를 JPA로 조회합니다.</p>
     *
     * @param tokenId 조회할 토큰 ID
     * @return 조회된 토큰 엔티티 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Token> findById(Long tokenId) {
        return tokenRepository.findById(tokenId);
    }

    /**
     * <h3>사용자의 모든 토큰 조회</h3>
     * <p>특정 사용자가 소유한 모든 Token 엔티티를 JPA로 조회합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자의 모든 토큰 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Token> findAllByUserId(Long userId) {
        return tokenRepository.findByUsersId(userId);
    }
}