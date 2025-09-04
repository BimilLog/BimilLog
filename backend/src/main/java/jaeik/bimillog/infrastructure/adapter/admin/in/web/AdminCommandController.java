package jaeik.bimillog.infrastructure.adapter.admin.in.web;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * <h2>관리자 명령 컨트롤러</h2>
 * <p>
 * 헥사고날 아키텍처의 Primary Adapter (Driving Adapter)
 * 사용자 차단, 강제 탈퇴 등 관리자 권한의 상태 변경 요청을 처리하는 웹 컨트롤러
 * </p>
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
     * <h3>사용자 제재 API</h3>
     * <p>신고 정보를 바탕으로 사용자를 제재 처리합니다.</p>
     *
     * @param reportDTO 신고 정보 DTO
     * @return ResponseEntity<String> 차단 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@RequestBody ReportDTO reportDTO) {
        adminCommandUseCase.banUser(reportDTO.toReportVO());
        return ResponseEntity.ok("유저를 성공적으로 차단했습니다.");
    }

    /**
     * <h3>사용자 강제 탈퇴 API</h3>
     * <p>관리자 권한으로 특정 사용자를 시스템에서 강제로 탈퇴 처리합니다.</p>
     *
     * @param userId 강제 탈퇴시킬 사용자 ID
     * @return ResponseEntity<String> 탈퇴 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @DeleteMapping("/withdraw/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceWithdrawUser(@PathVariable Long userId) {
        adminCommandUseCase.forceWithdrawUser(userId);
        return ResponseEntity.ok("관리자 권한으로 사용자 탈퇴가 완료되었습니다.");
    }
}
