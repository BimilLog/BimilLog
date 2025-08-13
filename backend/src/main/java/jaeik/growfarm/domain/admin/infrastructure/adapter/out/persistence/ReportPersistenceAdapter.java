package jaeik.growfarm.domain.admin.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.admin.application.port.out.LoadReportPort;
import jaeik.growfarm.domain.admin.entity.Report;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>신고 영속성 어댑터</h2>
 * <p>
 * 신고(`Report`) 관련 데이터를 영속화하고 조회하는 Outgoing-Adapter
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements LoadReportPort {

    private final ReportRepository reportRepository;

    @Override
    public Page<ReportDTO> findReportsWithPaging(ReportType reportType, Pageable pageable) {
        return reportRepository.findReportsWithPaging(reportType, pageable);
    }

    @Override
    public Optional<Report> findById(Long reportId) {
        return reportRepository.findById(reportId);
    }
}
