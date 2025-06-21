package jaeik.growfarm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>관리자 관련 컨트롤러</h2>
 * <p>
 * 신고 목록 조회
 * </p>
 * <p>
 * 신고 상세 조회
 * </p>
 * <p>
 * 유저 차단 및 블랙 리스트 등록
 * </p>
 */
@Tag(name = "관리자", description = "관리자 전용 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * <h3>신고 목록 조회 API</h3>
     *
     * @param page       페이지 번호
     * @param size       페이지 사이즈
     * @param reportType 신고 타입 (null이면 전체 조회)
     * @return 페이징 된 신고 목록
     * @since 2025-04-28
     */
    @Operation(summary = "신고 목록 조회", description = "신고된 내용의 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다.")
    })
    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportDTO>> getReportList(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam int page,
            @Parameter(description = "페이지 크기") @RequestParam int size,
            @Parameter(description = "신고 유형 (선택사항)") @RequestParam(required = false) ReportType reportType) {
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
    @Operation(summary = "신고 상세 조회", description = "특정 신고 내용의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 신고를 찾을 수 없습니다.")
    })
    @GetMapping("/report/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDTO> getReportDetail(
            @Parameter(description = "신고 ID") @PathVariable Long reportId) {
        ReportDTO reportDetail = adminService.getReportDetail(reportId);
        return ResponseEntity.ok(reportDetail);
    }

    /**
     * <h3>유저 차단 및 블랙 리스트 등록 API</h3>
     *
     * @param userId 유저 ID
     * @return 차단 완료 메시지
     * @since 2025-04-28
     */
    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단하고 블랙리스트에 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 차단 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "유저 차단 중 오류 발생")
    })
    @PostMapping("/user/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(
            @Parameter(description = "차단할 사용자 ID") @RequestParam Long userId) {
        adminService.banUser(userId);
        return ResponseEntity.ok("유저를 성공적으로 차단했습니다.");
    }
}
