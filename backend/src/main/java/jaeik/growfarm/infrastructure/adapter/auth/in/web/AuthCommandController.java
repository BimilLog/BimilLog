package jaeik.growfarm.infrastructure.adapter.auth.in.web;

import jaeik.growfarm.domain.auth.application.port.in.LogoutUseCase;
import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.in.SocialLoginUseCase;
import jaeik.growfarm.domain.auth.application.port.in.WithdrawUseCase;
import jaeik.growfarm.domain.common.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.LoginResponseDTO;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>인증 명령 컨트롤러</h2>
 * <p>소셜 로그인, 회원 가입, 로그아웃, 회원 탈퇴 등 인증 관련 명령 요청을 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthCommandController {

    private final SocialLoginUseCase socialLoginUseCase;
    private final SignUpUseCase signUpUseCase;
    private final LogoutUseCase logoutUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final AuthCookieManager authCookieManager;

    /**
     * <h3>소셜 로그인 API</h3>
     * <p>소셜 로그인 요청을 처리하고, 새로운 사용자라면 임시 UUID를 반환</p>
     *
     * @param provider 소셜 제공자 (예: KAKAO, NAVER 등)
     * @param code     소셜 로그인 인증 코드
     * @param fcmToken Firebase Cloud Messaging 토큰 (선택적)
     * @return 로그인 응답 DTO 또는 임시 UUID
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/login")
    public ResponseEntity<?> socialLogin(@RequestParam String provider, @RequestParam String code,
                                         @RequestParam(required = false) String fcmToken) {
        LoginResponseDTO<?> loginResponse = socialLoginUseCase.processSocialLogin(SocialProvider.valueOf(provider.toUpperCase()), code, fcmToken);

        if (loginResponse.getType() == LoginResponseDTO.LoginType.NEW_USER) {
            Map<String, String> uuidData = new HashMap<>();
            String uuid = (String) loginResponse.getData();
            uuidData.put("uuid", uuid);
            return ResponseEntity.ok().header("Set-Cookie", authCookieManager.createTempCookie(uuid).toString()).body(uuidData);
        }

        List<ResponseCookie> cookies = (List<ResponseCookie>) loginResponse.getData();
        return ResponseEntity.ok().headers(headers -> cookies.forEach(cookie ->
                headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }

    /**
     * <h3>회원 가입 API</h3>
     * <p>사용자가 회원 가입을 요청할 때 호출</p>
     *
     * @param userName 사용자의 이름
     * @param uuid     임시 UUID (소셜 로그인 후 생성된 UUID)
     * @return 회원 가입 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestParam String userName, @CookieValue String uuid) {
        return ResponseEntity.ok().headers(headers -> signUpUseCase.signUp(userName, uuid).forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }

    /**
     * <h3>로그아웃 API</h3>
     * <p>사용자가 로그아웃을 요청할 때 호출</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 로그아웃 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().headers(headers -> logoutUseCase.logout(userDetails).forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }

    /**
     * <h3>회원 탈퇴 API</h3>
     * <p>사용자가 회원 탈퇴를 요청할 때 호출</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 회원 탈퇴 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().headers(headers -> withdrawUseCase.withdraw(userDetails).forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).body("OK");
    }
}