package jaeik.bimillog.infrastructure.adapter.in.auth.web;

import jaeik.bimillog.domain.auth.application.port.in.LogoutUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SignUpUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialUseCase;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.global.annotation.Log;
import jaeik.bimillog.global.annotation.Log.LogLevel;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.AuthResponseDTO;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.SignUpRequestDTO;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>인증 명령 컨트롤러</h2>
 * <p>소셜 로그인, 회원 가입, 로그아웃 등 인증 관련 명령 요청을 처리합니다.</p>
 * <p>소셜 로그인, 회원가입, 로그아웃 API</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Validated
public class AuthCommandController {

    private final SocialUseCase socialUseCase;
    private final SignUpUseCase signUpUseCase;
    private final LogoutUseCase logoutUseCase;

    /**
     * <h3>소셜 로그인</h3>
     * <p>소셜 로그인 요청을 처리하고, 새로운 사용자라면 임시 UUID를 반환합니다.</p>
     *
     * @param request 소셜 로그인 요청 DTO (provider, code, fcmToken)
     * @return 로그인 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/login")
    @Log(level = LogLevel.INFO, 
         logExecutionTime = true,
         excludeParams = {"code", "fcmToken"},
         message = "소셜 로그인 요청")
    public ResponseEntity<AuthResponseDTO> socialLogin(@Valid @RequestBody SocialLoginRequestDTO request) {
        LoginResult loginResult = socialUseCase.processSocialLogin(
                request.getSocialProvider(), 
                request.getCode(), 
                request.getFcmToken());

        return switch (loginResult) {
            case LoginResult.NewUser(var uuid, var tempCookie) -> ResponseEntity.ok()
                    .header("Set-Cookie", tempCookie.toString())
                    .body(AuthResponseDTO.newUser(uuid));
            case LoginResult.ExistingUser(var cookies) -> ResponseEntity.ok()
                    .headers(headers -> cookies.forEach(cookie ->
                            headers.add("Set-Cookie", cookie.toString())))
                    .body(AuthResponseDTO.existingUser());
        };
    }

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
    @Log(level = LogLevel.INFO,
         logExecutionTime = true,
         excludeParams = {"uuid"},
         message = "회원가입 요청")
    public ResponseEntity<AuthResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO request) {
        return ResponseEntity.ok()
                .headers(headers -> signUpUseCase.signUp(request.getUserName(), request.getUuid()).forEach(cookie ->
                        headers.add("Set-Cookie", cookie.toString())))
                .body(AuthResponseDTO.success("회원 가입 성공"));
    }

    /**
     * <h3>로그아웃</h3>
     * <p>사용자의 로그아웃 요청을 처리합니다.</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 로그아웃 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/logout")
    @Log(level = LogLevel.INFO,
         message = "로그아웃 요청",
         logParams = false)
    public ResponseEntity<AuthResponseDTO> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok()
                .headers(headers -> logoutUseCase.logout(userDetails).forEach(cookie ->
                        headers.add("Set-Cookie", cookie.toString())))
                .body(AuthResponseDTO.success("로그아웃 성공"));
    }
}