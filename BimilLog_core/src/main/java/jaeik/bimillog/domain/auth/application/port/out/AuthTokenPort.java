package jaeik.bimillog.domain.auth.application.port.out;

/**
 * <h2>AuthToken 보안 명령 포트</h2>
 * <p>로그아웃/위험 탐지 시 리프레시 토큰을 무효화하거나 사용 이력을 기록합니다.</p>
 */
public interface AuthTokenPort {

    /**
     * <h3>선택적 토큰 삭제</h3>
     * <p>특정 기기에서 로그아웃할 때 해당 리프레시 토큰만 삭제하거나, 회원 탈퇴 시 모든 토큰을 제거합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null이면 사용자 전체 토큰 삭제)
     */
    void deleteTokens(Long memberId, Long tokenId);

}
