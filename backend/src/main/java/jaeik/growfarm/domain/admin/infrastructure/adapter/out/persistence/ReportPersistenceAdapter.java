package jaeik.growfarm.domain.admin.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.admin.application.port.out.LoadReportPort;
import jaeik.growfarm.domain.admin.domain.Report;
import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
