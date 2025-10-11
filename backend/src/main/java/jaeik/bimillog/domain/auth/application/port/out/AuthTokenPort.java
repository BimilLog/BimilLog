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

    /**
     * <h3>전 토큰 무효화</h3>
     * <p>보안 위협 감지 등 치명적인 상황에서 해당 회원의 모든 리프레시 토큰을 한 번에 삭제합니다.</p>
     *
     * @param memberId 회원 ID
     */
    void deleteAllByMemberId(Long memberId);

    /**
     * <h3>토큰 사용 이력 업데이트</h3>
     * <p>리프레시 토큰이 사용될 때 호출하여 마지막 사용 시각과 사용 횟수를 기록합니다.</p>
     *
     * @param tokenId 토큰 ID
     */
    void markTokenAsUsed(Long tokenId);

}
