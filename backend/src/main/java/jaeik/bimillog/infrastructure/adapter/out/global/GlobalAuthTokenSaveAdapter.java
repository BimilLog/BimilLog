package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.global.application.port.out.GlobalAuthTokenSavePort;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GlobalAuthTokenSaveAdapter implements GlobalAuthTokenSavePort {

    private final AuthTokenRepository authTokenRepository;

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     * <p>TokenRepository를 통해 데이터베이스에 토큰을 저장합니다.</p>
     *
     * @param authToken 저장할 토큰 엔티티
     * @return AuthToken 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public AuthToken save(AuthToken authToken) {
        return authTokenRepository.save(authToken);
    }

    /**
     * <h3>JWT 리프레시 토큰 업데이트</h3>
     * <p>AuthToken Rotation 시 JWT 리프레시 토큰을 갱신합니다.</p>
     * <p>리프레시 토큰이 재발급될 때 DB에 저장된 토큰 값을 업데이트합니다.</p>
     *
     * @param tokenId 토큰 ID
     * @param newJwtRefreshToken 새로운 JWT 리프레시 토큰
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void updateJwtRefreshToken(Long tokenId, String newJwtRefreshToken) {
        AuthToken authToken = authTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("AuthToken not found"));
        authToken.updateJwtRefreshToken(newJwtRefreshToken);
    }
}
