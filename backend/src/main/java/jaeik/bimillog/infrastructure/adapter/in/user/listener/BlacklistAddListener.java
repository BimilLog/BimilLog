package jaeik.bimillog.infrastructure.adapter.in.user.listener;

import jaeik.bimillog.domain.admin.event.UserForcedWithdrawalEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.user.application.port.in.WithdrawUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class BlacklistAddListener {

    private final WithdrawUseCase withdrawUseCase;

    /**
     * <h3>사용자 블랙리스트 이벤트 처리</h3>
     * <p>사용자 차단 또는 강제 탈퇴 이벤트를 수신하여 소셜 정보를 블랙리스트에 추가합니다.</p>
     * <p>처리 대상 이벤트:</p>
     * <ul>
     *   <li>UserBannedEvent: 사용자 차단 시</li>
     *   <li>UserForcedWithdrawalEvent: 관리자 강제 탈퇴 시</li>
     * </ul>
     *
     * @param event 사용자 차단 또는 강제 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener({UserBannedEvent.class, UserForcedWithdrawalEvent.class})
    @Transactional
    public void handleBlacklistEvents(Object event) {
        Long userId;
        String socialId;
        SocialProvider provider;
        String reason;
        
        if (event instanceof UserBannedEvent bannedEvent) {
            userId = bannedEvent.userId();
            socialId = bannedEvent.socialId();
            provider = bannedEvent.provider();
            reason = bannedEvent.reason();
        } else if (event instanceof UserForcedWithdrawalEvent userForcedWithdrawalEvent) {
            userId = userForcedWithdrawalEvent.userId();
            socialId = userForcedWithdrawalEvent.socialId();
            provider = userForcedWithdrawalEvent.provider();
            reason = userForcedWithdrawalEvent.reason();
        } else {
            log.warn("지원하지 않는 이벤트 타입: {}", event.getClass().getSimpleName());
            return;
        }

        try {
            if (event instanceof UserBannedEvent) {
                // 사용자 차단: BAN 상태로 변경 + 블랙리스트 등록
                withdrawUseCase.banUser(((UserBannedEvent) event).userId());
                withdrawUseCase.addToBlacklist(((UserBannedEvent) event).userId(), ((UserBannedEvent) event).socialId(), ((UserBannedEvent) event).provider());
                log.info("{} 처리 완료 - userId: {} (BAN 상태 변경 + 블랙리스트 등록)", reason, userId);
            } else {
                // 관리자 강제 탈퇴: 블랙리스트만 등록 (탈퇴는 다른 리스너에서 처리)
                withdrawUseCase.addToBlacklist(userId, socialId, provider);
                log.info("{} 블랙리스트 등록 완료 - userId: {}", reason, userId);
            }
        } catch (Exception e) {
            log.error("{} 처리 실패 - userId: {}, error: {}", 
                    reason, userId, e.getMessage(), e);
            throw e;
        }
    }
}