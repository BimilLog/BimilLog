package jaeik.growfarm.domain.admin.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportCustomRepository {
    Page<ReportDTO> findReportsWithPaging(ReportType reportType, Pageable pageable);
}
