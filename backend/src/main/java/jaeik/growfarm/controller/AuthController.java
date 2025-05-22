package jaeik.growfarm.controller;

import jaeik.growfarm.dto.user.FarmNameReqDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>인증 관련 컨트롤러</h2>
 * <p>카카오 로그인 처리</p>
 * <p>회원가입 처리</p>
 * <p>로그아웃 처리</p>
 * <p>회원 탈퇴 처리</p>
 * <p>현재 로그인한 유저 정보 조회</p>
 * <p>AWS 로드 밸런서 상태 검사</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * <h3>카카오 로그인 API</h3>
     *
     * @param code 프론트에서 반환된 카카오 인가 코드
     * @return JWT가 삽입된 쿠키 또는 토큰 ID
     * <p>기존 회원은 쿠키를 반환하고, 신규 회원은 토큰 ID를 반환한다.</p>
     * @since : 2025-04-28
     */
    @GetMapping("/login")
    public ResponseEntity<Object> loginKakao(@RequestParam String code) {
        Object result = authService.processKakaoLogin(code);

        if (result instanceof List<?> listResult) { // 반환값이 쿠키면 쿠키 반환 (기존 유저)
            List<ResponseCookie> cookies = (List<ResponseCookie>) listResult;
            return ResponseEntity.ok()
                    .header("Set-Cookie", cookies.get(0).toString())
                    .header("Set-Cookie", cookies.get(1).toString())
                    .build();
        } else {
            return ResponseEntity.ok(result); // 아니면 토큰 ID 반환 (신규 유저)
        }
    }

    /**
     * <h3>자체 서비스 회원 가입 API</h3>
     *
     * @param request 농장 등록 요청 DTO
     * @return JWT가 삽입된 쿠키
     * <p>카카오 로그인 후 신규 회원일 경우 작동되며 farmName과 tokenId를 받아서 쿠키를 반환한다.</p>
     * @since 2025-04-28
     */
    @PostMapping("/signUp")
    public ResponseEntity<Void> SignUp(@RequestBody @Valid FarmNameReqDTO request) {
        List<ResponseCookie> cookies = authService.signUp(request.getTokenId(), request.getFarmName());
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .build();
    }

    /**
     * <h3>로그아웃 API</h3>
     *
     * @return 로그아웃 완료 메시지
     * <p>쿠키를 삭제하여 자체 서비스 로그아웃</p>
     * <p>카카오 서버와 통신하여 카카오 로그아웃</p>
     * @since 2025-05-03
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ResponseCookie> cookies = authService.logout(userDetails);
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body("로그아웃 완료");
    }

    /**
     * <h3>회원 탈퇴 API</h3>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 회원탈퇴 완료 메시지
     * <p>쿠키를 삭제하고 관련 데이터를 삭제하여 자체 서비스 로그아웃</p>
     * <p>카카오 서버와 통신하여 카카오 연결끊기</p>
     * @since 2025-05-03
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ResponseCookie> cookies = authService.withdraw(userDetails);
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body("회원탈퇴 완료");
    }

    /**
     * <h3>현재 로그인한 사용자 정보 조회 API</h3>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 현재 로그인한 사용자 정보
     * <p>현재 로그인한 사용자의 정보를 조회하여 반환한다.</p>
     * @since 2025-05-03
     */
    @PostMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
        }
        return ResponseEntity.ok(userDetails.getUserDTO());
    }

    /**
     * <h3>서버 상태 검사 API</h3>
     *
     * @return 상태 검사 완료 메시지
     * @since 2025-04-28
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
