package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.auth.entity.AuthToken;


public interface GlobalAuthTokenSavePort {

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     * <p>새 토큰 생성이나 기존 토큰 정보 업데이트에 사용됩니다.</p>
     *
     * @param authToken 저장할 토큰 엔티티
     * @return AuthToken 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    AuthToken save(AuthToken authToken);

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
    void updateJwtRefreshToken(Long tokenId, String newJwtRefreshToken);
}
