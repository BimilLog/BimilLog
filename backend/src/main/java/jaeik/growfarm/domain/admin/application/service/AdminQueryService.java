
package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.growfarm.domain.admin.application.port.out.LoadReportPort;
import jaeik.growfarm.domain.admin.domain.Report;
import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminQueryService implements AdminQueryUseCase {

    private final LoadReportPort loadReportPort;

    @Override
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return loadReportPort.findReportsWithPaging(reportType, pageable);
    }

    @Override
    public ReportDTO getReportDetail(Long reportId) {
        Report report = loadReportPort.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REPORT));
        return ReportDTO.from(report);
    }
}
