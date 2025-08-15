package jaeik.growfarm.domain.notification.application.handler;

import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.growfarm.global.event.FcmTokenRegisteredEvent;
import jaeik.growfarm.global.event.UserLoggedOutEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>FCM 토큰 이벤트 핸들러</h2>
 * <p>FCM 토큰 관련 이벤트를 처리하는 Application Service</p>
 * <p>헥사고날 아키텍처를 준수하여 Use Case를 통해 비즈니스 로직 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenEventHandler {

    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>FCM 토큰 등록 이벤트 처리</h3>
     * <p>사용자 로그인 또는 회원가입 시 FCM 토큰을 등록합니다.</p>
     *
     * @param event FCM 토큰 등록 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void handleFcmTokenRegisteredEvent(FcmTokenRegisteredEvent event) {
        log.info("FCM 토큰 등록 이벤트 처리: userId={}", event.userId());
        notificationFcmUseCase.registerFcmToken(event.userId(), event.fcmToken());
    }

    /**
     * <h3>사용자 로그아웃 이벤트 처리</h3>
     * <p>사용자 로그아웃 시 FCM 토큰을 삭제합니다.</p>
     *
     * @param event 사용자 로그아웃 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void handleUserLoggedOutEvent(UserLoggedOutEvent event) {
        log.info("사용자 로그아웃 이벤트 처리: userId={}", event.userId());
        notificationFcmUseCase.deleteFcmTokens(event.userId());
    }

    /**
     * <h3>사용자 탈퇴 이벤트 처리</h3>
     * <p>사용자 탈퇴 시 FCM 토큰을 삭제합니다.</p>
     *
     * @param event 사용자 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void handleUserWithdrawnEvent(UserWithdrawnEvent event) {
        log.info("사용자 탈퇴 이벤트 처리: userId={}", event.userId());
        notificationFcmUseCase.deleteFcmTokens(event.userId());
    }
}