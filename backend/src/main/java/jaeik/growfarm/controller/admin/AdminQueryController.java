package jaeik.growfarm.controller.admin;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQueryController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<Page<ReportDTO>> getReportList(@RequestParam int page,
                                                         @RequestParam int size,
                                                         @RequestParam(required = false) ReportType reportType) {
        Page<ReportDTO> reportList = adminService.getReportList(page, size, reportType);
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
        ReportDTO reportDetail = adminService.getReportDetail(reportId);
        return ResponseEntity.ok(reportDetail);
    }
}
