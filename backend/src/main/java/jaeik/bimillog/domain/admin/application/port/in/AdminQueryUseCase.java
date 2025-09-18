package jaeik.bimillog.domain.admin.application.port.in;

import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.infrastructure.adapter.in.admin.web.AdminQueryController;
import org.springframework.data.domain.Page;

/**
 * <h2>관리자 조회 유스케이스</h2>
 * <p>관리자 도메인의 조회 작업을 담당하는 유스케이스입니다.</p>
 * <p>신고 목록 조회, 신고 통계 확인</p>
 * <p>관리자 대시보드에 필요한 읽기 전용 데이터 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface AdminQueryUseCase {

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>관리자 대시보드에서 신고 목록을 효율적으로 조회하고 관리할 수 있도록 지원합니다.</p>
     * <p>신고 유형별 필터링, 페이지네이션, 최신순 정렬</p>
     * <p>{@link AdminQueryController}에서 관리자 신고 관리 화면 요청 시 호출합니다.</p>
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 표시할 신고 수
     * @param reportType 필터링할 신고 유형 (null이면 전체 조회)
     * @return Page<Report> 페이지네이션된 신고 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    Page<Report> getReportList(int page, int size, ReportType reportType);
}
