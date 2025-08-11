package jaeik.growfarm.repository.admin;

import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportCustomRepository {
    Page<ReportDTO> findReportsWithPaging(ReportType reportType, Pageable pageable);
}
