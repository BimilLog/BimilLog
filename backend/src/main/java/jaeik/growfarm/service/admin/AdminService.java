package jaeik.growfarm.service.admin;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.ReportType;
import org.springframework.data.domain.Page;

public interface AdminService {

    Page<ReportDTO> getReportList(int page, int size, ReportType reportType);

    ReportDTO getReportDetail(Long reportId);

    void banUser(ReportDTO reportDTO);
}
