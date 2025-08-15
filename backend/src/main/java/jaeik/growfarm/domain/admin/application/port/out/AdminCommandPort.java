package jaeik.growfarm.domain.admin.application.port.out;


/**
 * <h2>Admin에서 사용자 인증 관리 포트</h2>
 * <p>관리자 권한으로 사용자 인증 관련 작업을 처리하기 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandPort {
    /**
     * <h3>관리자 권한으로 사용자 강제 탈퇴</h3>
     * 
     * @param userId 탈퇴시킬 사용자 ID
     */
    void forceWithdraw(Long userId);

    /**
     * <h3>사용자 ID로 모든 Emitter 삭제</h3>
     * <p>주어진 사용자 ID와 연결된 모든 SSE Emitter를 삭제합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllEmitterByUserId(Long userId);
}
