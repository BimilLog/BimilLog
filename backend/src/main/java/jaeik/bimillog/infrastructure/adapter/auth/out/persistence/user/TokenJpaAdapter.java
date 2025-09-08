package jaeik.bimillog.infrastructure.adapter.auth.out.persistence.user;

import jaeik.bimillog.domain.auth.application.port.out.LoadTokenPort;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.token.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * <h3>사용자의 모든 활성 토큰 조회</h3>
     * <p>특정 사용자가 소유한 모든 활성 토큰을 조회합니다.</p>
     * <p>회원 탈퇴 시 모든 토큰을 블랙리스트에 등록하기 위해 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @return 사용자의 모든 활성 토큰 목록
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<Token> findAllByUserId(Long userId) {
        return tokenRepository.findByUsersId(userId);
    }
}