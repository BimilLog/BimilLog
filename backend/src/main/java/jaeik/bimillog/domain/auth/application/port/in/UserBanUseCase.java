package jaeik.bimillog.domain.auth.application.port.in;

/**
 * <h2>유저 제재 유스케이스</h2>
 * <p>유저 강제탈퇴, 유저 제재를 관리하는 유스케이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserBanUseCase {

    /**
     * <h3>토큰 블랙리스트 여부 확인</h3>
     * <p>주어진 JWT 토큰이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     *
     * @param token JWT 토큰
     * @return 블랙리스트에 등록되어 있으면 true, 아니면 false
     * @since 2.0.0
     * @author Jaeik
     */
    boolean isBlacklisted(String token);

    /**
     * <h3>사용자의 모든 토큰을 블랙리스트에 등록</h3>
     * <p>특정 사용자의 모든 활성 토큰을 블랙리스트에 등록합니다.</p>
     * <p>주로 보안 위반이나 강제 로그아웃 시 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param reason 블랙리스트 등록 사유
     * @since 2.0.0
     * @author Jaeik
     */
    void blacklistAllUserTokens(Long userId, String reason);
}