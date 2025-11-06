package jaeik.bimillog.domain.member.in.web;

import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.member.application.port.in.MemberCommandUseCase;
import jaeik.bimillog.domain.member.application.port.in.MemberSignupUseCase;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.domain.member.event.ReportSubmittedEvent;
import jaeik.bimillog.domain.admin.in.dto.ReportDTO;
import jaeik.bimillog.domain.member.in.dto.MemberNameDTO;
import jaeik.bimillog.domain.member.in.dto.SettingDTO;
import jaeik.bimillog.domain.member.in.dto.SignUpRequestDTO;
import jaeik.bimillog.domain.auth.out.CustomUserDetails;
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
@RequestMapping("/api/member")
public class MemberCommandController {

    private final MemberCommandUseCase memberCommandUseCase;
    private final MemberSignupUseCase memberSignUpUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final GlobalCookiePort globalCookiePort;

    /**
     * <h3>회원가입</h3>
     * <p>사용자의 회원가입 요청을 처리합니다.</p>
     * <p>UUID는 HttpOnly 쿠키를 통해 전달받아 서버에서 추출합니다.</p>
     *
     * @param request 회원가입 요청 DTO (memberName)
     * @param uuid HttpOnly 쿠키로 전달된 임시 UUID
     * @return 회원 가입 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/signup")
    @Log(level = Log.LogLevel.INFO,
            logExecutionTime = true,
            excludeParams = {"uuid"},
            message = "회원가입 요청")
    public ResponseEntity<Void> signUp(
            @Valid @RequestBody SignUpRequestDTO request,
            @CookieValue(name = "temp_user_id") String uuid) {

        // 회원가입 로직 실행 후 쿠키 리스트 받기
        List<ResponseCookie> cookies = memberSignUpUseCase.signup(request.getMemberName(), uuid);

        // ResponseEntity builder 생성
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();

        // 쿠키를 하나씩 헤더에 추가
        for (ResponseCookie cookie : cookies) {
            responseBuilder.header("Set-Cookie", cookie.toString());
        }

        ResponseCookie expiredTempCookie = globalCookiePort.expireTempCookie();
        responseBuilder.header("Set-Cookie", expiredTempCookie.toString());

        // Body 설정 후 ResponseEntity 반환
        return responseBuilder.build();
    }

    /**
     * <h3>닉네임 변경 API</h3>
     * <p>사용자의 닉네임을 변경하는 요청을 처리</p>
     *
     * @param userDetails 사용자 인증 정보
     * @param memberNameDTO 닉네임 DTO
     * @return 닉네임 변경 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/username")
    public ResponseEntity<String> updateUserName(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @RequestBody @Valid MemberNameDTO memberNameDTO) {
        memberCommandUseCase.updateMemberName(userDetails.getMemberId(), memberNameDTO.getMemberName());
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    /**
     * <h3>설정 수정 API</h3>
     * <p>사용자의 설정을 수정하는 요청을 처리</p>
     * <p>클라이언트에서 POST /api/member/setting 요청 시 호출됩니다.</p>
     *
     * @param settingDTO  설정 DTO
     * @param userDetails 사용자 인증 정보
     * @return 설정 수정 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(@RequestBody @Valid SettingDTO settingDTO,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberCommandUseCase.updateMemberSettings(userDetails.getMemberId(), settingDTO.toSettingEntity());
        return ResponseEntity.ok("설정 수정 완료");
    }

    /**
     * <h3>신고/건의사항 제출 API</h3>
     * <p>인증된 사용자와 익명 사용자 모두 신고나 건의사항을 제출할 수 있는 요청을 처리</p>
     * <p>클라이언트에서 POST /api/member/report 요청 시 호출됩니다.</p>
     * <ul>
     *   <li>POST: 게시글 신고 - targetId 필수</li>
     *   <li>COMMENT: 댓글 신고 - targetId 필수</li>
     *   <li>ERROR: 오류 신고 - targetId 불필요</li>
     *   <li>IMPROVEMENT: 기능 개선 건의 - targetId 불필요</li>
     *   <li>익명 신고: 인증되지 않은 사용자도 신고 가능</li>
     * </ul>
     *
     * @param reportDTO 신고 정보 DTO
     * @return 신고 제출 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/report")
    public ResponseEntity<String> submitReport(@RequestBody @Valid ReportDTO reportDTO) {
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
     * <p>클라이언트에서 DELETE /api/member/withdraw 요청 시 호출됩니다.</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 회원 탈퇴 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        eventPublisher.publishEvent(new MemberWithdrawnEvent(userDetails.getMemberId(), userDetails.getSocialId(), userDetails.getSocialProvider()));
        return ResponseEntity.ok()
                .headers(headers -> globalCookiePort.getLogoutCookies().forEach(cookie ->
                        headers.add("Set-Cookie", cookie.toString())))
                .build();
    }
}