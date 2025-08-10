package jaeik.growfarm.controller.auth;

import jaeik.growfarm.dto.auth.LoginResponseDTO;
import jaeik.growfarm.entity.user.SocialProvider;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>인증 관련 Command 컨트롤러</h2>
 * <p>로그인, 회원가입, 로그아웃, 회원탈퇴 등 인증 관련 상태 변경 요청을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthCommandController {

    private final AuthService authService;

    /**
     * <h3>소셜 로그인 API</h3>
     *
     * <p>기존 회원은 JWT를 담은 쿠키를 반환하고, 신규 회원은 UUID가 있는 임시 쿠키를 반환한다.</p>
     *
     * @param provider 소셜 로그인 제공자 (kakao, google 등)
     * @param code 프론트에서 반환된 인가 코드
     * @param fcmToken (선택) FCM 토큰, 푸시 알림을 위한 토큰
     * @return Jwt가 삽입된 쿠키 또는 임시 쿠키
     * @author Jaeik
     * @since 3.0.0
     */
    @PostMapping("/login/{provider}")
    public ResponseEntity<?> loginSocial(@PathVariable SocialProvider provider,
                                         @RequestParam String code,
                                         @RequestParam(required = false) String fcmToken) {
        LoginResponseDTO<?> result = authService.processSocialLogin(provider, code, fcmToken);

        if (result.getType() == LoginResponseDTO.LoginType.EXISTING_USER) {
            return createCookieResponse((List<ResponseCookie>) result.getData(), "로그인 성공");
        } else {
            return createCookieResponse((ResponseCookie) result.getData(), "임시 쿠키 발급 완료");
        }
    }

    /**
     * <h3>서비스 회원 가입 API</h3>
     *
     * <p>카카오 로그인 후 신규 회원일 경우 작동되며 userName과 uuid를 받아서 Jwt 쿠키를 반환한다.</p>
     *
     * @param userName 닉네임
     * @param uuid 임시 쿠키에 저장된 UUID
     * @return JWT가 삽입된 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/signUp")
    public ResponseEntity<String> SignUp(@RequestParam String userName,
                                         @CookieValue(value = "temp") String uuid) {
        List<ResponseCookie> cookies = authService.signUp(userName, uuid);
        return createCookieResponse(cookies, "회원 가입 완료 - JWT 쿠키 발급됨");
    }

    /**
     * <h3>로그아웃 API</h3>
     *
     * <p>모든 로그아웃 처리를 수행하고 로그아웃 쿠키를 반환한다.</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 로그아웃 쿠키
     * @author Jaeik
     * @since 3.0.0
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ResponseCookie> cookies = authService.logout(userDetails);
        return createCookieResponse(cookies, "로그아웃 완료");
    }

    /**
     * <h3>회원 탈퇴 API</h3>
     * <p>쿠키를 삭제하고 관련 유저정보를 삭제하여 자체 서비스 회원 탈퇴</p>
     * <p>카카오 서버와 통신하여 카카오 연결끊기</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 로그아웃 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ResponseCookie> cookies = authService.withdraw(userDetails);
        return createCookieResponse(cookies, "회원탈퇴 완료");
    }

    private ResponseEntity<String> createCookieResponse(List<ResponseCookie> cookies, String body) {
        return ResponseEntity.ok()
                .header("Set-Cookie", cookies.get(0).toString())
                .header("Set-Cookie", cookies.get(1).toString())
                .body(body);
    }

    private ResponseEntity<String> createCookieResponse(ResponseCookie cookie, String body) {
        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(body);
    }
}
