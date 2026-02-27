package jaeik.bimillog.domain.auth.contoller;

import jaeik.bimillog.domain.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.domain.auth.dto.LoginResultDTO;
import jaeik.bimillog.domain.auth.event.MemberLoggedOutEvent;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialLoginService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.web.HTTPCookie;
import jaeik.bimillog.domain.notification.dto.FcmTokenRegisterRequestDTO;
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
@Log(level = LogLevel.INFO, logExecutionTime = true, logParams = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Validated
public class AuthCommandController {
    private final SocialLoginService socialLoginService;
    private final HTTPCookie HTTPCookie;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthTokenService authTokenService;

    /**
     * <h3>소셜 로그인</h3>
     * <p>소셜 로그인 요청을 처리합니다. 신규/기존 회원 모두 즉시 JWT 토큰을 발급합니다.</p>
     *
     * @param request 소셜 로그인 요청 DTO (provider, code)
     * @return 로그인 응답
     */
    @Log(level = LogLevel.INFO, logExecutionTime = true, excludeParams = {"code", "state"}, message = "소셜 로그인 요청")
    @PostMapping("/login")
    public ResponseEntity<Void> socialLogin(@Valid @RequestBody SocialLoginRequestDTO request) {
        LoginResultDTO loginResultDTO = socialLoginService.processSocialLogin(request.getProvider(), request.getCode(), request.getState());

        return ResponseEntity.ok()
                .headers(headers -> HTTPCookie
                        .generateJwtCookie(loginResultDTO.getJwtAccessToken(), loginResultDTO.getJwtRefreshToken())
                        .forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).build();
    }

    /**
     * <h3>로그아웃</h3>
     * <p>사용자의 로그아웃 요청을 처리합니다.</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @return 로그아웃 성공 응답
     */
    @Log(level = LogLevel.INFO, message = "로그아웃 요청", logParams = false)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        eventPublisher.publishEvent(new MemberLoggedOutEvent(userDetails.getMemberId(), userDetails.getAuthTokenId(), userDetails.getSocialProvider()));
        return ResponseEntity.ok().headers(headers -> HTTPCookie.getLogoutCookies()
                .forEach(cookie -> headers.add("Set-Cookie", cookie.toString()))).build();
    }

    /**
     * <h3>FCM 토큰 등록</h3>
     * <p>사용자의 FCM 푸시 알림 토큰을 해당 기기의 AuthToken에 등록합니다.</p>
     * <p>로그인 또는 회원가입 완료 후 클라이언트가 별도로 호출합니다.</p>
     *
     * @param userDetails 인증된 사용자 정보 (memberId, authTokenId 포함)
     * @param request     FCM 토큰 등록 요청 DTO
     * @return 등록 성공 응답
     */
    @Log(level = LogLevel.INFO, logExecutionTime = true, message = "FCM 토큰 등록", excludeParams = {"fcmToken"}, logParams = false)
    @PostMapping("/fcm")
    public ResponseEntity<Void> registerFcmToken(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @Valid @RequestBody FcmTokenRegisterRequestDTO request) {
        authTokenService.registerFcmToken(userDetails.getAuthTokenId(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }
}
