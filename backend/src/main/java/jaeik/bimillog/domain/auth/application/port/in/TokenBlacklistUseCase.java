package jaeik.bimillog.domain.auth.application.port.in;

/**
 * <h2>토큰 블랙리스트 유스케이스</h2>
 * <p>JWT 토큰 블랙리스트 관리를 위한 인터페이스</p>
 * <p>헥사고날 아키텍처의 드라이빙 포트로 사용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface TokenBlacklistUseCase {

    /**
     * <h3>토큰 블랙리스트 여부 확인</h3>
     * <p>주어진 JWT 토큰이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     *
     * @param token JWT 토큰
     * @return 블랙리스트에 등록되어 있으면 true, 아니면 false
     */
    boolean isBlacklisted(String token);

    /**
     * <h3>사용자의 모든 토큰을 블랙리스트에 등록</h3>
     * <p>특정 사용자의 모든 활성 토큰을 블랙리스트에 등록합니다.</p>
     * <p>주로 보안 위반이나 강제 로그아웃 시 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param reason 블랙리스트 등록 사유
     */
    void blacklistAllUserTokens(Long userId, String reason);
}