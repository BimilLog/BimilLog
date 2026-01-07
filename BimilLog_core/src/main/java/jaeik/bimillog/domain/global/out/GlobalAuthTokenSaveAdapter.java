package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.out.AuthTokenRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>글로벌 AuthToken 저장 어댑터</h2>
 * <p>소셜 로그인, 회원가입, JWT 회전 등 다양한 진입점에서 동일 저장 로직을 재사용합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalAuthTokenSaveAdapter {
    private final AuthTokenRepository authTokenRepository;

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
    @Transactional
    public void updateJwtRefreshToken(Long tokenId, String newJwtRefreshToken) {
        AuthToken authToken = authTokenRepository.findById(tokenId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_TOKEN_NOT_FOUND));
        authToken.updateJwtRefreshToken(newJwtRefreshToken);
    }
}
