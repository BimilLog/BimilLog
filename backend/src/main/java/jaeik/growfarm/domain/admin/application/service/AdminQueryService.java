
package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.growfarm.domain.admin.application.port.out.AdminQueryPort;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
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

    @Override
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminQueryPort.findReportsWithPaging(reportType, pageable);
    }
}
