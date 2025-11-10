package jaeik.bimillog.domain.notification.controller;

import jaeik.bimillog.domain.auth.out.CustomUserDetails;
import jaeik.bimillog.domain.global.service.GlobalFcmSaveService;
import jaeik.bimillog.domain.notification.dto.FcmTokenRegisterRequestDTO;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.log.Log.LogLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>알림 FCM 컨트롤러</h2>
 * <p>FCM 푸시 알림 토큰 등록 요청을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationFcmController {

    private final GlobalFcmSaveService globalFcmSaveService;

    /**
     * <h3>FCM 토큰 등록</h3>
     * <p>사용자의 FCM 푸시 알림 토큰을 서버에 등록합니다.</p>
     * <p>로그인 또는 회원가입 완료 후 클라이언트가 별도로 호출합니다.</p>
     *
     * @param userDetails 인증된 사용자 정보
     * @param request     FCM 토큰 등록 요청 DTO
     * @return 등록 성공 응답
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/fcm")
    @Log(level = LogLevel.INFO,
         message = "FCM 토큰 등록",
         excludeParams = {"fcmToken"})
    public ResponseEntity<Void> registerFcmToken(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @Valid @RequestBody FcmTokenRegisterRequestDTO request) {
        globalFcmSaveService.registerFcmToken(userDetails.getMemberId(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }
}

