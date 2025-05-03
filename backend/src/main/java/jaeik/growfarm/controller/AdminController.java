package jaeik.growfarm.controller;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/*
 * 관리자 전용 API
 * 신고 목록 조회
 * 신고 상세 조회
 * 유저 차단 및 블랙 리스트 등록
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    /*
     * 신고 목록 조회 API
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * param ReportType reportType: 신고 타입 (null이면 전체 조회)
     * return: Page<ReportDTO>
     * 수정일 : 2025-04-28
     */
    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportDTO>> getReportList(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) ReportType reportType) {
        Page<ReportDTO> reportList = adminService.getReportList(page, size, reportType);
        return ResponseEntity.ok(reportList);
    }

    /*
     * 신고 상세 조회 API
     * param Long reportId: 신고 ID
     * return: ReportDTO
     * 수정일 : 2025-04-28
     */
    @GetMapping("/report/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDTO> getReportDetail(@PathVariable Long reportId) {
        ReportDTO reportDetail = adminService.getReportDetail(reportId);
        return ResponseEntity.ok(reportDetail);
    }

    /*
     * 유저 차단 및 블랙 리스트 등록 API
     * param Long userId: 유저 ID
     * return: ResponseEntity<String>
     * 수정일 : 2025-04-28
     */
    @PostMapping("/user/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@RequestParam Long userId) {
        adminService.banUser(userId);
        return ResponseEntity.ok("유저가 차단되었습니다.");
    }
}
