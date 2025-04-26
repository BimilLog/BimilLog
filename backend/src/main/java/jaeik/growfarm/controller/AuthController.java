package jaeik.growfarm.controller;

import jaeik.growfarm.dto.user.FarmNameReqDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

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

    @PostMapping("/signUp")
    public ResponseEntity<Void> SignUp(@RequestBody FarmNameReqDTO request) {
        List<ResponseCookie> cookies = authService.signUp(request.getTokenId(), request.getFarmName());

        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        List<ResponseCookie> cookies = authService.logout();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body("로그아웃 완료");
    }

    // 회원 탈퇴
    @GetMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest().body("인증 정보가 없습니다. 다시 로그인해주세요.");
            }

            List<ResponseCookie> cookies = authService.withdraw(userDetails);

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookies.get(0).toString())
                    .header("Set-Cookie", cookies.get(1).toString())
                    .body("회원탈퇴 완료");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("회원탈퇴 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        UserDTO userDTO = authService.getCurrentUser();

        if (userDTO == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }

        return ResponseEntity.ok(userDTO);
    }

    //AWS 로드 밸런서 상태 검사
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
