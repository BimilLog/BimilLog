package jaeik.growfarm.controller;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>관리자 전용 API</h2>
 * <p>신고 목록 조회</p>
 * <p>신고 상세 조회</p>
 * <p>유저 차단 및 블랙 리스트 등록</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * <h3>신고 목록 조회 API</h3>
     * @param page 페이지 번호,
     * @param size 페이지 사이즈
     * @param reportType 신고 타입 (null이면 전체 조회)
     * @return 페이징 된 신고 목록
     * @since 2025-04-28
     */
    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportDTO>> getReportList(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) ReportType reportType) {
        Page<ReportDTO> reportList = adminService.getReportList(page, size, reportType);
        return ResponseEntity.ok(reportList);
    }

    /**
     * <h2>신고 상세 조회 API</h2>
     * @param reportId 신고 ID
     * @return 신고 상세 정보
     * @since 2025-04-28
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
