package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.out.AuthTokenRepository;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>GlobalAuthTokenAdapter</h2>
 * <p>외부에서 접근하는 AuthToken을 담당하는 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Component
@RequiredArgsConstructor
public class GlobalAuthTokenAdapter {
    private final AuthTokenService authTokenService;

    /**
     * <h3>토큰 ID로 토큰 조회</h3>
     * <p>특정 ID에 해당하는 토큰 엔티티를 조회합니다.</p>
     *
     * @param tokenId 조회할 토큰 ID
     * @return Optional&lt;AuthToken&gt; 조회된 토큰 객체 (존재하지 않으면 Optional.empty())
     */
    public Optional<AuthToken> findById(Long tokenId) {
        return authTokenService.findById(tokenId);
    }

    /**
     * <h3>JWT 리프레시 토큰 업데이트</h3>
     * <p>JWT 리프레시 토큰을 갱신합니다.</p>
     *
     * @param tokenId 토큰 ID
     * @param newJwtRefreshToken 새로운 JWT 리프레시 토큰
     */
    @Transactional
    public void updateJwtRefreshToken(Long tokenId, String newJwtRefreshToken) {
        authTokenService.updateJwtRefreshToken(tokenId, newJwtRefreshToken);
    }
}