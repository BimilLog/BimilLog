package jaeik.bimillog.domain.admin.application.port.in;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import org.springframework.data.domain.Page;

/**
 * <h2>관리자 조회 유스케이스</h2>
 * <p>관리자 기능 중 사용자 및 신고 정보 조회를 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminQueryUseCase {

    /**
     * <h3>신고 목록 조회</h3>
     * <p>신고 목록을 페이지네이션하여 조회합니다. 특정 신고 유형에 따라 필터링할 수 있습니다.</p>
     *
     * @param page       페이지 번호
     * @param size       페이지 크기
     * @param reportType 신고 유형 (선택 사항)
     * @return Page<Report> 신고 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<Report> getReportList(int page, int size, ReportType reportType);
}
