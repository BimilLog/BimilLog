package jaeik.growfarm.domain.admin.application.port.in;

import jaeik.growfarm.domain.admin.entity.ReportVO;

/**
 * <h2>관리자 명령 유스케이스</h2>
 * <p>관리자 기능 중 사용자의 상태를 변경하는 명령 요청을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandUseCase {
    /**
     * <h3>사용자 제재</h3>
     * <p>주어진 신고 정보를 기반으로 사용자를 제재합니다.</p>
     *
     * @param reportVO 신고 정보 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    void banUser(ReportVO reportVO);

    /**
     * <h3>사용자 강제 탈퇴</h3>
     * <p>주어진 사용자 ID에 해당하는 사용자를 강제로 탈퇴 처리합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void forceWithdrawUser(Long userId);
}
