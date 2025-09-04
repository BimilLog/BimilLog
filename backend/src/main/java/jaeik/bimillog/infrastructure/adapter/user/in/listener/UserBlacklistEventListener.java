package jaeik.bimillog.infrastructure.adapter.user.in.listener;

import jaeik.bimillog.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 블랙리스트 이벤트 리스너</h2>
 * <p>사용자 차단 및 강제 탈퇴 시 소셜 정보를 블랙리스트에 등록하여 재가입을 방지하는 리스너입니다.</p>
 * <p>User 도메인의 블랙리스트 관리 책임을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserBlacklistEventListener {

    private final UserCommandUseCase userCommandUseCase;

    /**
     * <h3>사용자 블랙리스트 이벤트 처리</h3>
     * <p>사용자 차단 또는 강제 탈퇴 이벤트를 수신하여 소셜 정보를 블랙리스트에 추가합니다.</p>
     * <p>처리 대상 이벤트:</p>
     * <ul>
     *   <li>UserBannedEvent: 사용자 차단 시</li>
     *   <li>AdminWithdrawRequestedEvent: 관리자 강제 탈퇴 시</li>
     * </ul>
     *
     * @param event 사용자 차단 또는 강제 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener({UserBannedEvent.class, AdminWithdrawRequestedEvent.class})
    public void handleBlacklistEvents(ApplicationEvent event) {
        Long userId;
        String eventType;
        
        if (event instanceof UserBannedEvent bannedEvent) {
            userId = bannedEvent.getUserId();
            eventType = "사용자 차단";
        } else if (event instanceof AdminWithdrawRequestedEvent withdrawEvent) {
            userId = withdrawEvent.userId();
            eventType = "관리자 강제 탈퇴";
        } else {
            log.warn("지원하지 않는 이벤트 타입: {}", event.getClass().getSimpleName());
            return;
        }
        
        log.info("{} 이벤트 수신 - 블랙리스트 등록 시작: userId={}", eventType, userId);
        
        try {
            userCommandUseCase.addToBlacklist(userId);
            log.info("{} 블랙리스트 등록 완료 - userId: {}", eventType, userId);
        } catch (Exception e) {
            log.error("{} 블랙리스트 등록 실패 - userId: {}, error: {}", 
                    eventType, userId, e.getMessage(), e);
            throw e;
        }
    }
}