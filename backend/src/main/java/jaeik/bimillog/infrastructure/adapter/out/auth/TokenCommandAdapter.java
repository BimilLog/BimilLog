package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.TokenCommandPort;
import jaeik.bimillog.domain.auth.entity.JwtToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>토큰 명령 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 토큰 쓰기 기능을 구현하는 어댑터입니다.</p>
 * <p>GlobalTokenCommandPort를 구현하여 도메인 간 토큰 쓰기 기능을 통합 제공합니다.</p>
 * <p>TokenRepository를 통해 실제 토큰 데이터를 저장/삭제합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class TokenCommandAdapter implements TokenCommandPort {

    private final JwtTokenRepository jwtTokenRepository;

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     * <p>TokenRepository를 통해 데이터베이스에 토큰을 저장합니다.</p>
     *
     * @param jwtToken 저장할 토큰 엔티티
     * @return JwtToken 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public JwtToken save(JwtToken jwtToken) {
        return jwtTokenRepository.save(jwtToken);
    }

    /**
     * <h3>토큰 삭제</h3>
     * <p>로그아웃시 특정 토큰만 삭제</p>
     * <p>회원탈퇴시 모든 토큰 삭제</p>
     * <p>{@link WithdrawService}에서 특정 토큰 정리 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void deleteTokens(Long userId, Long tokenId) {
        if (tokenId != null) {
            jwtTokenRepository.deleteById(tokenId);
        } else {
            jwtTokenRepository.deleteAllByUserId(userId);
        }
    }

    /**
     * <h3>JWT 리프레시 토큰 업데이트</h3>
     * <p>JwtToken Rotation 시 JWT 리프레시 토큰을 갱신합니다.</p>
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
        JwtToken jwtToken = jwtTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("JwtToken not found"));
        jwtToken.updateJwtRefreshToken(newJwtRefreshToken);
    }

    /**
     * <h3>사용자의 모든 토큰 삭제</h3>
     * <p>보안 위협 감지 시 특정 사용자의 모든 활성 토큰을 무효화합니다.</p>
     * <p>리프레시 토큰 탈취 또는 재사용 공격 감지 시 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteAllByUserId(Long userId) {
        jwtTokenRepository.deleteAllByUserId(userId);
    }
}