package jaeik.bimillog.domain.auth.contoller;

import jaeik.bimillog.domain.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
import jaeik.bimillog.domain.auth.service.SocialLoginService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.log.Log.LogLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
@Log(level = LogLevel.INFO,
        logExecutionTime = true,
        logParams = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Validated
public class AuthCommandController {

    private final SocialLoginService socialLoginService;
    private final GlobalCookieAdapter globalCookieAdapter;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * <h3>소셜 로그인</h3>
     * <p>소셜 로그인 요청을 처리하고, 새로운 사용자라면 임시 UUID를 반환합니다.</p>
     *
     * @param request 소셜 로그인 요청 DTO (provider, code)
     * @return 로그인 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/login")
    @Log(level = LogLevel.INFO,
         logExecutionTime = true,
         excludeParams = {"code", "state"},
         message = "소셜 로그인 요청")
    public ResponseEntity<String> socialLogin(@Valid @RequestBody SocialLoginRequestDTO request) {
        LoginResult loginResult = socialLoginService.processSocialLogin(
                request.getSocialProvider(),
                request.getCode(),
                request.getState());

        return switch (loginResult) {
            case LoginResult.NewUser(var tempCookie) -> ResponseEntity.ok()
                    .header("Set-Cookie", tempCookie.toString())
                    .body("NEW_USER");
            case LoginResult.ExistingUser(var cookies) -> ResponseEntity.ok()
                    .headers(headers -> cookies.forEach(cookie ->
                            headers.add("Set-Cookie", cookie.toString())))
                    .body("EXISTING_USER");
        };
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
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        eventPublisher.publishEvent(new MemberLoggedOutEvent(userDetails.getMemberId(), userDetails.getAuthTokenId(), userDetails.getSocialProvider()));
        return ResponseEntity.ok()
                .headers(headers -> globalCookieAdapter.getLogoutCookies().forEach(cookie ->
                        headers.add("Set-Cookie", cookie.toString())))
                .build();
    }
}
