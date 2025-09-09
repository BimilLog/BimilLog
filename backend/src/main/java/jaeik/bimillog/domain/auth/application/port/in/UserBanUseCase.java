package jaeik.bimillog.domain.auth.application.port.in;

/**
 * <h2>유저 제재 유스케이스</h2>
 * <p>
 * 사용자 차단과 토큰 블랙리스트 관리를 처리하는 비즈니스 로직의 진입점입니다.
 * </p>
 * <p>JWT 토큰 검증 필터에서 토큰 유효성 확인과 AdminService에서 사용자 제재 처리 시 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UserBanUseCase {

    /**
     * <h3>토큰 블랙리스트 여부 확인</h3>
     * <p>주어진 JWT 토큰이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>차단된 사용자나 강제 로그아웃된 토큰의 접근을 차단하는데 사용됩니다.</p>
     * <p>JwtAuthenticationFilter에서 모든 인증 요청 시 토큰 유효성 검증을 위해 호출됩니다.</p>
     *
     * @param token 검증할 JWT 토큰 문자열
     * @return 블랙리스트에 등록되어 있으면 true, 정상 토큰이면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isBlacklisted(String token);

    /**
     * <h3>사용자의 모든 토큰을 블랙리스트에 등록</h3>
     * <p>특정 사용자의 모든 활성 토큰을 블랙리스트에 등록하여 즉시 로그아웃 처리합니다.</p>
     * <p>사용자가 모든 기기에서 강제 로그아웃되어 재로그인이 필요하게 됩니다.</p>
     * <p>AdminService에서 사용자 차단 처리 시 또는 UserService에서 회원 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param userId 강제 로그아웃할 사용자 ID
     * @param reason 블랙리스트 등록 사유 (예: "사용자 차단", "회원 탈퇴")
     * @author Jaeik
     * @since 2.0.0
     */
    void blacklistAllUserTokens(Long userId, String reason);
}