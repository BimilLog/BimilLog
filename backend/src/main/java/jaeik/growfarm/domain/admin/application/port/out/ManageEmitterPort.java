package jaeik.growfarm.domain.admin.application.port.out;

/**
 * <h2>Emitter 관리 포트</h2>
 * <p>관리자 도메인에서 Emitter(SSE 연결) 관리를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ManageEmitterPort {

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
