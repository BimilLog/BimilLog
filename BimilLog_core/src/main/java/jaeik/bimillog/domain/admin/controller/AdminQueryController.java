package jaeik.bimillog.domain.admin.controller;

import jaeik.bimillog.domain.admin.service.AdminQueryService;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.dto.ReportDTO;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>관리자 조회 컨트롤러</h2>
 * <p>관리자 도메인의 조회 작업을 담당하는 컨트롤러입니다.</p>
 * <p>신고 목록 페이지네이션 조회</p>
 * <p>ADMIN 권한 필요</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        message = "관리자 API 요청")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminQueryController {
    private final AdminQueryService adminQueryService;

    /**
     * <h3>신고 목록 조회 API</h3>
     * <p>최신순으로 정렬된 데이터를 제공합니다.</p>
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지당 신고 수 (기본값: 20, 최대 100)
     * @param reportType 필터링할 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT 중 선택, null이면 전체)
     * @return ResponseEntity<Page<ReportDTO>> 페이지네이션된 신고 목록과 메타데이터
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportDTO>> getReportList(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(required = false) ReportType reportType) {
        Page<ReportDTO> reportList = adminQueryService.getReportList(page, size, reportType);
        return ResponseEntity.ok(reportList);
    }
}
