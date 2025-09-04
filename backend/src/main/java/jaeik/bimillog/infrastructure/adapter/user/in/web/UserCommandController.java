package jaeik.bimillog.infrastructure.adapter.user.in.web;

import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.SettingDTO;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.UserNameDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>사용자 명령 컨트롤러</h2>
 * <p>사용자 관련 명령 요청을 처리하는 컨트롤러</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserCommandController {

    private final UserCommandUseCase userCommandUseCase;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>닉네임 변경 API</h3>
     * <p>사용자의 닉네임을 변경하는 요청을 처리</p>
     *
     * @param userDetails 사용자 인증 정보
     * @param userNameDTO 닉네임 DTO
     * @return 닉네임 변경 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @PostMapping("/username")
    public ResponseEntity<String> updateUserName(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @RequestBody @Valid UserNameDTO userNameDTO) {
        userCommandUseCase.updateUserName(userDetails.getUserId(), userNameDTO.getUserName());
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    /**
     * <h3>설정 수정 API</h3>
     * <p>사용자의 설정을 수정하는 요청을 처리</p>
     *
     * @param settingDTO 설정 DTO
     * @param userDetails 사용자 인증 정보
     * @return 설정 수정 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(@RequestBody SettingDTO settingDTO,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        userCommandUseCase.updateUserSettings(userDetails.getUserId(), settingDTO.toSettingEntity());
        return ResponseEntity.ok("설정 수정 완료");
    }

    /**
     * <h3>신고/건의사항 제출 API</h3>
     * <p>인증된 사용자와 익명 사용자 모두 신고나 건의사항을 제출할 수 있는 요청을 처리</p>
     * <ul>
     *   <li>POST: 게시글 신고 - targetId 필수</li>
     *   <li>COMMENT: 댓글 신고 - targetId 필수</li>
     *   <li>SUGGESTION: 건의사항 - targetId 불필요</li>
     *   <li>익명 신고: 인증되지 않은 사용자도 신고 가능</li>
     * </ul>
     *
     * @param reportDTO   신고 정보 DTO
     * @param userDetails 사용자 인증 정보 (익명일 경우 null)
     * @return 신고 제출 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @PostMapping("/report")
    public ResponseEntity<String> submitReport(@RequestBody @Valid ReportDTO reportDTO,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 인증된 사용자와 익명 사용자 구분 처리
        Long reporterId = (userDetails != null) ? userDetails.getUserId() : null;
        String reporterName = (userDetails != null) ? userDetails.getUsername() : "익명";
        
        // 신고 이벤트 발행
        ReportSubmittedEvent event = ReportSubmittedEvent.of(
                reporterId,
                reporterName,
                reportDTO.toReportVO()
        );
        
        eventPublisher.publishEvent(event);
        
        return ResponseEntity.ok("신고/건의사항이 접수되었습니다.");
    }
}