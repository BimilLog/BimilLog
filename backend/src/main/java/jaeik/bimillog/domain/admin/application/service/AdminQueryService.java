
package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.bimillog.domain.admin.application.port.out.AdminQueryPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.infrastructure.adapter.in.admin.web.AdminQueryController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * <h2>관리자 조회 서비스</h2>
 * <p>관리자 도메인의 조회 작업을 담당하는 서비스입니다.</p>
 * <p>신고 목록 페이지네이션 조회</p>
 * <p>읽기 전용 트랜잭션 사용</p>
 * <p>신고 유형별 필터링, 최신순 정렬</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class AdminQueryService implements AdminQueryUseCase {

    private final AdminQueryPort adminQueryPort;

    /**
     * <h3>신고 목록 페이지네이션 조회</h3>
     * <p>관리자 대시보드의 신고 관리 화면에 데이터를 제공합니다.</p>
     * <p>신고 유형별 필터링, 최신순 정렬, Spring Data JPA Pageable 활용</p>
     * <p>{@link AdminQueryController}에서 관리자 신고 목록 조회 시 호출됩니다.</p>
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 표시할 신고 수
     * @param reportType 필터링할 신고 유형 (null이면 전체 조회)
     * @return Page<Report> 페이지네이션된 신고 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<Report> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminQueryPort.findReportsWithPaging(reportType, pageable);
    }
}
