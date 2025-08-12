package jaeik.growfarm.domain.admin.infrastructure.adapter.in.web;

import jaeik.growfarm.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.dto.admin.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>관리자 관련 Query 컨트롤러</h2>
 * <p>신고 목록 조회 등 관리자 권한의 조회 요청을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/query")
public class AdminQueryController {

    private final AdminQueryUseCase adminQueryUseCase;

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportDTO>> getReportList(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) ReportType reportType) {
        Page<ReportDTO> reportList = adminQueryUseCase.getReportList(page, size, reportType);
        return ResponseEntity.ok(reportList);
    }

    /**
     * <h3>신고 상세 조회 API</h3>
     *
     * @param reportId 신고 ID
     * @return 신고 상세 정보
     * @since 2025-04-28
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDTO> getReportDetail(@PathVariable Long reportId) {
        ReportDTO reportDetail = adminQueryUseCase.getReportDetail(reportId);
        return ResponseEntity.ok(reportDetail);
    }
}
