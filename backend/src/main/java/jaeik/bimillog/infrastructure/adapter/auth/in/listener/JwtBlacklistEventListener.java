package jaeik.bimillog.infrastructure.adapter.auth.in.listener;

import jaeik.bimillog.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.TokenBlacklistUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>JWT 블랙리스트 이벤트 리스너</h2>
 * <p>사용자 차단 및 강제 탈퇴 시 해당 사용자의 모든 JWT 토큰을 블랙리스트에 등록하여 즉시 무효화하는 리스너입니다.</p>
 * <p>Auth 도메인의 토큰 무효화 책임을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JwtBlacklistEventListener {

    private final TokenBlacklistUseCase tokenBlacklistUseCase;

    /**
     * <h3>JWT 블랙리스트 이벤트 처리</h3>
     * <p>사용자 차단 또는 강제 탈퇴 이벤트를 수신하여 해당 사용자의 모든 JWT 토큰을 무효화합니다.</p>
     * <p>처리 대상 이벤트:</p>
     * <ul>
     *   <li>UserBannedEvent: 사용자 차단 시 - "사용자 제재" 사유로 토큰 무효화</li>
     *   <li>AdminWithdrawRequestedEvent: 관리자 강제 탈퇴 시 - "관리자 강제 탈퇴" 사유로 토큰 무효화</li>
     * </ul>
     *
     * @param event 사용자 차단 또는 강제 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener({UserBannedEvent.class, AdminWithdrawRequestedEvent.class})
    public void handleJwtBlacklistEvents(ApplicationEvent event) {
        Long userId;
        String reason;
        
        if (event instanceof UserBannedEvent bannedEvent) {
            userId = bannedEvent.getUserId();
            reason = "사용자 제재";
        } else if (event instanceof AdminWithdrawRequestedEvent withdrawEvent) {
            userId = withdrawEvent.userId();
            reason = "관리자 강제 탈퇴";
        } else {
            log.warn("지원하지 않는 이벤트 타입: {}", event.getClass().getSimpleName());
            return;
        }
        
        log.info("{} 이벤트 수신 - JWT 토큰 무효화 시작: userId={}", reason, userId);
        
        try {
            tokenBlacklistUseCase.blacklistAllUserTokens(userId, reason);
            log.info("{} JWT 토큰 무효화 완료 - userId: {}", reason, userId);
        } catch (Exception e) {
            log.error("{} JWT 토큰 무효화 실패 - userId: {}, error: {}", 
                    reason, userId, e.getMessage(), e);
            throw e;
        }
    }
}