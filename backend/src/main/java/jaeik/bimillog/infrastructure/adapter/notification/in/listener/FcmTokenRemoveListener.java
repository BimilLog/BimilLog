package jaeik.bimillog.infrastructure.adapter.notification.in.listener;

import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>FCM 토큰 제거 이벤트 리스너</h2>
 * <p>사용자 로그아웃 및 탈퇴 시 FCM 토큰을 제거하는 리스너입니다.</p>
 * <p>Notification 도메인의 FCM 토큰 관리 책임을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenRemoveListener {

    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>FCM 토큰 제거 이벤트 처리</h3>
     * <p>사용자 로그아웃 또는 탈퇴 이벤트를 수신하여 FCM 토큰을 삭제합니다.</p>
     * <p>처리 대상 이벤트:</p>
     * <ul>
     *   <li>UserLoggedOutEvent: 사용자 로그아웃 시</li>
     *   <li>UserWithdrawnEvent: 사용자 탈퇴 시</li>
     * </ul>
     *
     * @param event 사용자 로그아웃 또는 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener({UserLoggedOutEvent.class, UserWithdrawnEvent.class})
    @Transactional
    public void handleFcmTokenRemovalEvents(Object event) {
        Long userId;
        String eventType;
        
        if (event instanceof UserLoggedOutEvent logoutEvent) {
            userId = logoutEvent.userId();
            eventType = "사용자 로그아웃";
        } else if (event instanceof UserWithdrawnEvent withdrawnEvent) {
            userId = withdrawnEvent.userId();
            eventType = "사용자 탈퇴";
        } else {
            log.warn("지원하지 않는 이벤트 타입: {}", event.getClass().getSimpleName());
            return;
        }
        
        log.info("{} 이벤트 처리 시작 - FCM 토큰 삭제: 사용자 ID={}", eventType, userId);
        
        try {
            notificationFcmUseCase.deleteFcmTokens(userId);
            log.info("{} FCM 토큰 삭제 완료 - 사용자 ID={}", eventType, userId);
        } catch (Exception e) {
            log.error("{} FCM 토큰 삭제 실패 - 사용자 ID={}, error: {}", 
                    eventType, userId, e.getMessage(), e);
            throw e;
        }
    }
}