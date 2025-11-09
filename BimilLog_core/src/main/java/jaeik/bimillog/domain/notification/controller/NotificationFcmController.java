package jaeik.bimillog.domain.notification.controller;

import jaeik.bimillog.domain.auth.out.CustomUserDetails;
import jaeik.bimillog.domain.global.service.GlobalFcmSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationFcmController {

    private final GlobalFcmSaveService globalFcmSaveService;

    @PostMapping("/fcm")
    public ResponseEntity<String> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                  String fcmToken) {
        globalFcmSaveService.registerFcmToken(userDetails.getMemberId(), fcmToken);
        return ResponseEntity.ok().build();
    }
}

