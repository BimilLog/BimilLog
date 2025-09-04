package jaeik.bimillog.domain.admin.application.port.in;

import jaeik.bimillog.domain.admin.entity.ReportType;

/**
 * <h2>관리자 명령 유스케이스</h2>
 * <p>관리자 기능 중 사용자의 상태를 변경하는 명령 요청을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminCommandUseCase {
    /**
     * <h3>신고/건의사항 저장</h3>
     * <p>사용자가 제출한 신고나 건의사항을 저장합니다.</p>
     *
     * @param userId     신고자 ID
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @param content    신고 내용
     * @author Jaeik
     * @since 2.0.0
     */
    void createReport(Long userId, ReportType reportType, Long targetId, String content);

    /**
     * <h3>사용자 제재</h3>
     * <p>주어진 신고 정보를 기반으로 사용자를 제재합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void banUser(ReportType reportType, Long targetId);

    /**
     * <h3>사용자 강제 탈퇴</h3>
     * <p>주어진 신고 정보를 기반으로 사용자를 강제로 탈퇴 처리합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void forceWithdrawUser(ReportType reportType, Long targetId);
}
