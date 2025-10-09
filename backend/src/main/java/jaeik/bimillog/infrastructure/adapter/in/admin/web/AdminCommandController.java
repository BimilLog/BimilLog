package jaeik.bimillog.infrastructure.adapter.in.admin.web;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.infrastructure.adapter.in.admin.dto.BanUserDTO;
import jaeik.bimillog.infrastructure.adapter.in.admin.dto.ForceWithdrawDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * <h2>관리자 명령 컨트롤러</h2>
 * <p>관리자 도메인의 명령 작업을 담당하는 컨트롤러입니다.</p>
 * <p>사용자 제재, 강제 탈퇴 처리</p>
 * <p>ADMIN 권한 필요</p>
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
     * <p>ADMIN 권한이 있는 관리자만 접근할 수 있으며, 신고 유형과 대상 ID를 기반으로 사용자를 식별합니다.</p>
     * <p>AdminCommandUseCase.banUser를 호출하여 도메인 계층에서 제재 로직을 실행하고 UserBannedEvent를 발행합니다.</p>
     * <p>성공적으로 제재 처리되면 200 OK와 함께 완료 메시지를 반환합니다.</p>
     *
     * @param banUserDTO 사용자 제재 DTO (신고 유형, 대상 ID 포함)
     * @return ResponseEntity<String> 제재 완료 응답 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@RequestBody BanUserDTO banUserDTO) {
        adminCommandUseCase.banUser(banUserDTO.getReportType(), banUserDTO.getTargetId());
        return ResponseEntity.ok("유저를 성공적으로 차단했습니다.");
    }

    /**
     * <h3>사용자 강제 탈퇴 API</h3>
     * <p>관리자 대시보드에서 심각한 위반으로 판단하여 사용자를 영구적으로 시스템에서 제거하는 REST API입니다.</p>
     * <p>단순 제재보다 강력한 최종 조치입니다.</p>
     * <p>AdminCommandUseCase.forceWithdrawUser를 호출하여 MemberWithdrawnEvent를 발행하고 탈퇴 처리를 위임합니다.</p>
     *
     * @param forceWithdrawDTO 강제 탈퇴 DTO (신고 유형, 대상 ID 포함)
     * @return ResponseEntity<String> 강제 탈퇴 완료 응답 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceWithdrawUser(@RequestBody ForceWithdrawDTO forceWithdrawDTO) {
        adminCommandUseCase.forceWithdrawUser(forceWithdrawDTO.getReportType(), forceWithdrawDTO.getTargetId());
        return ResponseEntity.ok("사용자 탈퇴 처리를 시작했습니다. 백그라운드에서 처리됩니다.");
    }
}
