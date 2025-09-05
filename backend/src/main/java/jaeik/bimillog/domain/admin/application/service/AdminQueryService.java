
package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.bimillog.domain.admin.application.port.out.AdminQueryPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>관리자 조회 서비스</h2>
 * <p>관리자 조회 유스케이스(`AdminQueryUseCase`)를 구현하는 서비스 클래스</p>
 * <p>신고 목록 조회 및 신고 상세 조회와 같은 관리자 기능을 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminQueryService implements AdminQueryUseCase {

    private final AdminQueryPort adminQueryPort;

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
    @Override
    public Page<Report> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminQueryPort.findReportsWithPaging(reportType, pageable);
    }
}
