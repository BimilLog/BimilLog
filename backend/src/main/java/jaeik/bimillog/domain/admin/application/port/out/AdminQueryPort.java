package jaeik.bimillog.domain.admin.application.port.out;

import jaeik.bimillog.domain.admin.entity.ReportSummary;
import jaeik.bimillog.domain.admin.entity.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>신고 조회 포트</h2>
 * <p>관리자 도메인에서 신고(`Report`) 엔티티 조회를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminQueryPort {

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>주어진 신고 유형에 따라 신고 목록을 페이지네이션하여 조회합니다.</p>
     *
     * @param reportType 신고 유형 (null 가능, 전체 조회 시)
     * @param pageable   페이지 정보
     * @return Page<ReportSummary> 신고 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<ReportSummary> findReportsWithPaging(ReportType reportType, Pageable pageable);

}
