package jaeik.growfarm.domain.admin.application.port.in;

import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import org.springframework.data.domain.Page;

public interface AdminQueryUseCase {

    Page<ReportDTO> getReportList(int page, int size, ReportType reportType);

    ReportDTO getReportDetail(Long reportId);
}
