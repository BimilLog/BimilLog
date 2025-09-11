package jaeik.bimillog.infrastructure.adapter.notification.in.listener;

import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>FCM 토큰 제거 이벤트 리스너</h2>
 * <p>로그아웃과 회원 탈퇴 이벤트를 수신하여 FCM 토큰을 삭제하는 리스너입니다.</p>
 * <p>사용자 로그아웃 시 FCM 토큰 삭제, 회원 탈퇴 시 FCM 토큰 삭제</p>
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
     * <h3>로그아웃/탈퇴 완료 시 사용자 FCM 토큰 정리</h3>
     * <p>사용자가 로그아웃 버튼을 클릭하거나 회원탈퇴 절차를 완료하는 상황에서 
     * AuthCommandService가 로그아웃 처리나 탈퇴 처리를 완료한 후 해당 이벤트를 발행하면, 
     * 이를 수신하여 해당 사용자의 모든 디바이스에 등록된 FCM 토큰을 삭제합니다.</p>
     * <p>로그아웃한 사용자에게 계속해서 푸시 알림이 전송되는 것을 방지하고, 
     * 탈퇴한 사용자의 개인정보(FCM 토큰) 완전 삭제를 보장하기 위해 NotificationFcmUseCase의 
     * deleteFcmTokens 메서드를 호출하여 DB에서 토큰 레코드를 제거합니다.</p>
     * <p>처리 대상: UserLoggedOutEvent(로그아웃), UserWithdrawnEvent(회원탈퇴)</p>
     *
     * @param event 사용자 로그아웃 완료 또는 회원탈퇴 완료 이벤트 (사용자 ID 포함)
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