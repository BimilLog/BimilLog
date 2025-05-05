package jaeik.growfarm.controller;

import jaeik.growfarm.dto.user.FarmNameReqDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 인증 관련 API
 * 카카오 로그인 처리
 * 회원가입 처리
 * 로그아웃 처리
 * 회원 탈퇴 처리
 * 현재 로그인한 유저 정보 조회
 * AWS 로드 밸런서 상태 검사
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    /*
     * 카카오 로그인 API
     * param String code: 프론트에서 반환된 카카오 인가 코드
     * return: ResponseEntity<Object> 쿠키 또는 토큰 ID (토큰 테이블의 PK)
     * 기존 회원은 쿠키를 반환하고, 신규 회원은 토큰 ID를 반환한다.
     * 수정일 : 2025-04-28
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

    /*
     * 자체 서비스 회원 가입 API
     * param FarmNameReqDTO request: farmName, tokenId (사용자가 입력한 농장 이름, 카카오 로그인 후 반환된 토큰 ID)
     * return: ResponseEntity<Void> 쿠키
     * 카카오 로그인 후 신규 회원가입 시 farmName과 tokenId를 받아서 쿠키를 반환한다.
     * 수정일 : 2025-04-28
     */
    @PostMapping("/signUp")
    public ResponseEntity<Void> SignUp(@RequestBody FarmNameReqDTO request) {
        List<ResponseCookie> cookies = authService.signUp(request.getTokenId(), request.getFarmName());
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .build();
    }

    /*
     * 로그아웃 API
     * param: 없음
     * return: ResponseEntity<String> 로그아웃 완료 메시지
     * 자체 서비스와 카카오 모두 로그아웃 한다. 쿠키를 삭제한다.
     * 수정일 : 2025-05-03
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ResponseCookie> cookies = authService.logout(userDetails);
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body("로그아웃 완료");
    }

    /*
     * 회원 탈퇴 API
     * param: CustomUserDetails userDetails (인증된 사용자 정보)
     * return: ResponseEntity<String> 회원탈퇴 완료 메시지
     * 자체 서비스와 카카오 모두 회원탈퇴 한다. 쿠키를 삭제한다.
     * 수정일 : 2025-05-03
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
            List<ResponseCookie> cookies = authService.withdraw(userDetails);
            return ResponseEntity.ok()
                    .header("Set-Cookie", cookies.get(0).toString())
                    .header("Set-Cookie", cookies.get(1).toString())
                    .body("회원탈퇴 완료");
    }

    /*
     * 현재 로그인한 사용자 정보 조회 API
     * param: CustomUserDetails userDetails (인증된 사용자 정보)
     * return: ResponseEntity<UserDTO> 현재 로그인한 사용자 정보
     * 현재 로그인한 사용자의 정보를 조회하여 프론트에 반환한다.
     * 수정일 : 2025-05-03
     */
    @PostMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }
        return ResponseEntity.ok(userDetails.getUserDTO());
    }

    /*
     * AWS ALB 상태 검사 API
     * param: 없음
     * return: ResponseEntity<String> 상태 검사 결과
     * AWS ALB에서 상태 검사를 위해 사용한다.
     * 수정일 : 2025-04-28
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
