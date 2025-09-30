package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.entity.JwtToken;

/**
 * <h2>토큰 명령 포트</h2>
 * <p>토큰 저장, 토큰 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface TokenCommandPort {

    /**
     * <h3>토큰 저장</h3>
     * <p>토큰 정보를 저장하거나 업데이트합니다.</p>
     * <p>새 토큰 생성이나 기존 토큰 정보 업데이트에 사용됩니다.</p>
     *
     * @param jwtToken 저장할 토큰 엔티티
     * @return JwtToken 저장된 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    JwtToken save(JwtToken jwtToken);

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
    void deleteTokens(Long userId, Long tokenId);

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
    void updateJwtRefreshToken(Long tokenId, String newJwtRefreshToken);

    /**
     * <h3>사용자의 모든 토큰 삭제</h3>
     * <p>보안 위협 감지 시 특정 사용자의 모든 활성 토큰을 무효화합니다.</p>
     * <p>리프레시 토큰 탈취 또는 재사용 공격 감지 시 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByUserId(Long userId);

    /**
     * <h3>토큰 사용 기록</h3>
     * <p>리프레시 토큰이 사용될 때마다 호출되어 사용 이력을 기록합니다.</p>
     * <p>재사용 공격 감지를 위해 사용 횟수를 증가시키고 마지막 사용 시각을 업데이트합니다.</p>
     * <p>트랜잭션 내에서 실행되어 DB에 즉시 반영됩니다.</p>
     *
     * @param tokenId 토큰 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void markTokenAsUsed(Long tokenId);

}