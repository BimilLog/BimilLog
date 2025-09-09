package jaeik.bimillog.infrastructure.adapter.admin.in.web;

import jaeik.bimillog.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.infrastructure.adapter.admin.dto.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>AdminQueryController</h2>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 조회 처리를 위한 Primary Adapter입니다.
 * </p>
 * <p>
 * CQRS 패턴에 따른 쿼리(Query) 측면의 관리자 API를 제공하며, 읽기 전용 관리자 업무를 처리합니다.
 * </p>
 * <p>
 * 관리자 대시보드에서 신고 목록 조회, 통계 확인 등의 요청을 받아 적절한 데이터를 제공합니다.
 * </p>
 * <p>
 * Spring Security의 @PreAuthorize를 통해 ADMIN 권한을 가진 사용자만 접근할 수 있도록 보안을 강화합니다.
 * </p>
 * <p>
 * AdminQueryUseCase를 호출하여 도메인 계층에서 데이터를 조회하고, 
 * 웹 계층에 적합한 DTO 형태로 변환하여 클라이언트에 응답합니다.
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
     * <h3>신고 목록 페이지네이션 조회 API</h3>
     * <p>관리자 대시보드에서 신고 관리 화면을 표시하기 위한 신고 목록 조회 REST API입니다.</p>
     * <p>프론트엔드에서 GET /api/admin/reports?page=0&size=10&reportType=POST 형태로 요청됩니다.</p>
     * <p>ADMIN 권한이 있는 관리자만 접근할 수 있으며, 페이지네이션과 신고 유형별 필터링을 지원합니다.</p>
     * <p>AdminQueryUseCase.getReportList를 호출하여 도메인에서 신고 데이터를 조회합니다.</p>
     * <p>도메인 엔티티인 Report를 웹 계층에 적합한 ReportDTO로 변환하여 클라이언트에 응답합니다.</p>
     * <p>관리자가 효율적으로 신고를 검토하고 제재 여부를 결정할 수 있도록 최신순으로 정렬된 데이터를 제공합니다.</p>
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지당 신고 수 (기본값: 10, 최대 100)
     * @param reportType 필터링할 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT 중 선택, null이면 전체)
     * @return ResponseEntity<Page<ReportDTO>> 페이지네이션된 신고 목록과 메타데이터
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportDTO>> getReportList(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) ReportType reportType) {
        Page<Report> reports = adminQueryUseCase.getReportList(page, size, reportType);
        Page<ReportDTO> reportList = reports.map(ReportDTO::from);
        return ResponseEntity.ok(reportList);
    }
}
