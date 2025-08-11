package jaeik.growfarm.domain.admin.application.port.out;

import jaeik.growfarm.domain.admin.domain.Report;
import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LoadReportPort {

    Page<ReportDTO> findReportsWithPaging(ReportType reportType, Pageable pageable);

    Optional<Report> findById(Long reportId);
}
