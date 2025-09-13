package jaeik.bimillog.infrastructure.adapter.auth.in.listener;

import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.application.port.in.UserBanUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>JWT 블랙리스트 이벤트 리스너</h2>
 * <p>사용자 차단 및 강제 탈퇴 시 해당 사용자의 모든 JWT 토큰을 블랙리스트에 등록하여 무효화하는 리스너입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtBlacklistListener {

    private final UserBanUseCase userBanUseCase;

    /**
     * <h3>JWT 블랙리스트 이벤트 처리</h3>
     * <p>사용자 차단 또는 강제 탈퇴 이벤트를 수신하여 해당 사용자의 모든 JWT 토큰을 무효화합니다.</p>
     * <p>UserBannedEvent: 사용자 제재, AdminWithdrawEvent: 관리자 강제 탈퇴</p>
     *
     * @param event 사용자 차단 또는 강제 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener({UserBannedEvent.class, AdminWithdrawEvent.class})
    @Transactional
    public void handleJwtBlacklistEvents(Object event) {
        Long userId;
        String reason;
        
        if (event instanceof UserBannedEvent bannedEvent) {
            userId = bannedEvent.userId();
            reason = "사용자 제재";
        } else if (event instanceof AdminWithdrawEvent withdrawEvent) {
            userId = withdrawEvent.userId();
            reason = "관리자 강제 탈퇴";
        } else {
            log.warn("지원하지 않는 이벤트 타입: {}", event.getClass().getSimpleName());
            return;
        }
        
        log.info("{} 이벤트 수신 - JWT 토큰 무효화 시작: userId={}", reason, userId);
        
        try {
            userBanUseCase.blacklistAllUserTokens(userId, reason);
            log.info("{} JWT 토큰 무효화 완료 - userId: {}", reason, userId);
        } catch (Exception e) {
            log.error("{} JWT 토큰 무효화 실패 - userId: {}, error: {}", 
                    reason, userId, e.getMessage(), e);
            throw e;
        }
    }
}