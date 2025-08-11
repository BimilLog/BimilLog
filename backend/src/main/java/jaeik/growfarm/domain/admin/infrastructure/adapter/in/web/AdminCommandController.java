package jaeik.growfarm.domain.admin.infrastructure.adapter.in.web;

import jaeik.growfarm.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.growfarm.dto.admin.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>관리자 관련 Command 컨트롤러</h2>
 * <p>사용자 차단 등 관리자 권한의 상태 변경 요청을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminCommandController {

    private final AdminCommandUseCase adminCommandUseCase;

    /**
     * <h3>유저 차단 및 블랙 리스트 등록 API</h3>
     *
     * @param reportDTO 신고 DTO
     * @return 차단 완료 메시지
     * @since 2025-04-28
     */
    @PostMapping("/reports/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@RequestBody ReportDTO reportDTO) {
        adminCommandUseCase.banUser(reportDTO);
        return ResponseEntity.ok("유저를 성공적으로 차단했습니다.");
    }
}
