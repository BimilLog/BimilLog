package jaeik.bimillog.infrastructure.adapter.in.user.web;

import jaeik.bimillog.domain.user.application.port.in.SignUpUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import jaeik.bimillog.global.annotation.Log;
import jaeik.bimillog.infrastructure.adapter.in.admin.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.AuthResponseDTO;
import jaeik.bimillog.infrastructure.adapter.in.user.dto.SettingDTO;
import jaeik.bimillog.infrastructure.adapter.in.user.dto.SignUpRequestDTO;
import jaeik.bimillog.infrastructure.adapter.in.user.dto.UserNameDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final SignUpUseCase signUpUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final WithdrawUseCase withdrawUseCase;

    /**
     * <h3>회원가입</h3>
     * <p>사용자의 회원가입 요청을 처리합니다.</p>
     *
     * @param request 회원가입 요청 DTO (userName, uuid)
     * @return 회원 가입 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/signup")
    @Log(level = Log.LogLevel.INFO,
            logExecutionTime = true,
            excludeParams = {"uuid"},
            message = "회원가입 요청")
    public ResponseEntity<AuthResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO request) {

        // 회원가입 로직 실행 후 쿠키 리스트 받기
        List<ResponseCookie> cookies = signUpUseCase.signUp(request.getUserName(), request.getUuid());

        // ResponseEntity builder 생성
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();

        // 쿠키를 하나씩 헤더에 추가
        for (ResponseCookie cookie : cookies) {
            responseBuilder.header("Set-Cookie", cookie.toString());
        }

        // Body 설정 후 ResponseEntity 반환
        return responseBuilder.body(AuthResponseDTO.success("회원 가입 성공"));
    }


    /**
     * <h3>닉네임 변경 API</h3>
     * <p>사용자의 닉네임을 변경하는 요청을 처리</p>
     * <p>클라이언트에서 POST /api/user/username 요청 시 호출됩니다.</p>
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
     * <p>클라이언트에서 POST /api/user/setting 요청 시 호출됩니다.</p>
     *
     * @param settingDTO 설정 DTO
     * @param userDetails 사용자 인증 정보
     * @return 설정 수정 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(@RequestBody @Valid SettingDTO settingDTO,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        userCommandUseCase.updateUserSettings(userDetails.getUserId(), settingDTO.toSettingEntity());
        return ResponseEntity.ok("설정 수정 완료");
    }

    /**
     * <h3>신고/건의사항 제출 API</h3>
     * <p>인증된 사용자와 익명 사용자 모두 신고나 건의사항을 제출할 수 있는 요청을 처리</p>
     * <p>클라이언트에서 POST /api/user/report 요청 시 호출됩니다.</p>
     * <ul>
     *   <li>POST: 게시글 신고 - targetId 필수</li>
     *   <li>COMMENT: 댓글 신고 - targetId 필수</li>
     *   <li>ERROR: 오류 신고 - targetId 불필요</li>
     *   <li>IMPROVEMENT: 기능 개선 건의 - targetId 불필요</li>
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
        // 신고자 정보 설정 (인증된 사용자 또는 익명 사용자)
        reportDTO.enrichReporterInfo(userDetails);
        
        // 신고 이벤트 발행
        ReportSubmittedEvent event = ReportSubmittedEvent.of(
                reportDTO.getReporterId(),
                reportDTO.getReporterName(),
                reportDTO.getReportType(),
                reportDTO.getTargetId(),
                reportDTO.getContent()
        );
        
        eventPublisher.publishEvent(event);
        
        return ResponseEntity.ok("신고/건의사항이 접수되었습니다.");
    }

    /**
     * <h3>회원 탈퇴 API</h3>
     * <p>사용자가 회원 탈퇴를 요청할 때 호출</p>
     * <p>클라이언트에서 DELETE /api/user/withdraw 요청 시 호출됩니다.</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 회원 탈퇴 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<AuthResponseDTO> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok()
                .headers(headers -> withdrawUseCase.withdraw(userDetails).forEach(cookie ->
                        headers.add("Set-Cookie", cookie.toString())))
                .body(AuthResponseDTO.success("회원탈퇴 성공"));
    }
}