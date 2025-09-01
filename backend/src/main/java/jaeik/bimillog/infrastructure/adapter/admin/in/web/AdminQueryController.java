package jaeik.bimillog.infrastructure.adapter.admin.in.web;

import jaeik.bimillog.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.bimillog.domain.admin.entity.ReportSummary;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>관리자 조회 컨트롤러</h2>
 * <p>
 * 헥사고날 아키텍처의 Primary Adapter (Driving Adapter)
 * 신고 목록 조회 등 관리자 권한의 조회 요청을 처리하는 웹 컨트롤러
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminQueryController {

    private final AdminQueryUseCase adminQueryUseCase;

    /**
     * <h3>신고 목록 조회 API</h3>
     * <p>신고 목록을 페이지네이션하여 조회합니다. 특정 신고 유형에 따라 필터링할 수 있습니다.</p>
     * <p>도메인 ReportSummary를 웹 계층의 ReportDTO로 변환하여 반환합니다.</p>
     *
     * @param page       페이지 번호 (0부터 시작, 기본값: 0)
     * @param size       페이지 크기 (기본값: 10)
     * @param reportType 신고 유형 필터 (선택 사항, null이면 전체 조회)
     * @return ResponseEntity<Page<ReportDTO>> 신고 목록 페이지 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportDTO>> getReportList(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) ReportType reportType) {
        Page<ReportSummary> reportSummaries = adminQueryUseCase.getReportList(page, size, reportType);
        Page<ReportDTO> reportList = reportSummaries.map(reportSummary -> ReportDTO.builder()
                .id(reportSummary.id())
                .reporterId(reportSummary.reporterId())
                .reporterName(reportSummary.reporterName())
                .reportType(reportSummary.reportType())
                .targetId(reportSummary.targetId())
                .content(reportSummary.content())
                .createdAt(reportSummary.createdAt())
                .build());
        return ResponseEntity.ok(reportList);
    }
}
