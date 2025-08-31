package jaeik.bimillog.infrastructure.adapter.admin.in.web;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


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
     * @since 2.0.0
     * @author Jaeik
     */
    @PostMapping("/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@RequestBody ReportDTO reportDTO) {
        adminCommandUseCase.banUser(reportDTO.toReportVO());
        return ResponseEntity.ok("유저를 성공적으로 차단했습니다.");
    }

    /**
     * <h3>관리자 권한으로 사용자 강제 탈퇴 API</h3>
     *
     * @param userId 탈퇴시킬 사용자 ID
     * @return 탈퇴 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @DeleteMapping("/withdraw/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceWithdrawUser(@PathVariable Long userId) {
        adminCommandUseCase.forceWithdrawUser(userId);
        return ResponseEntity.ok("관리자 권한으로 사용자 탈퇴가 완료되었습니다.");
    }
}
