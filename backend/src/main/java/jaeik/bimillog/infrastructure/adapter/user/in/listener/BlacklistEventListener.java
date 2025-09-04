package jaeik.bimillog.infrastructure.adapter.user.in.listener;

import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 서비스</h2>
 * <p>사용자 관련 이벤트를 처리하는 서비스 클래스입니다.</p>
 * <p>사용자가 차단되었을 때 블랙리스트에 추가하는 기능을 제공합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BlacklistEventListener {

    private final UserCommandUseCase userCommandUseCase;

    /**
     * <h3>사용자 차단 이벤트 처리</h3>
     * <p>사용자가 차단되었을 때 다음 작업을 수행합니다:</p>
     * <ul>
     *   <li>사용자 역할을 BAN으로 변경 (일정 기간 서비스 이용 제한)</li>
     *   <li>블랙리스트에 추가 (재가입 방지)</li>
     * </ul>
     * <p>UserCommandUseCase를 통해 비즈니스 로직을 위임하여 일관성을 유지합니다.</p>
     *
     * @param event 차단된 사용자 이벤트
     * @since 2.0.0
     * @author jaeik
     */
    @Async
    @EventListener
    public void handleUserBannedEvent(UserBannedEvent event) {
        log.info("사용자 제재 이벤트 처리 시작: userId={}, 소셜 ID={}, 제공자={}", 
                event.getUserId(), event.getSocialId(), event.getProvider());
        
        try {
            // 1. 사용자 역할을 BAN으로 변경 (일정 기간 제재)
            userCommandUseCase.banUser(event.getUserId());
            
            // 2. 블랙리스트에 추가 (재가입 방지)
            userCommandUseCase.addToBlacklist(event.getUserId());
            
            log.info("사용자 제재 처리 완료 - userId: {}, 역할 변경: BAN, 블랙리스트 추가 완료", 
                    event.getUserId());
        } catch (Exception e) {
            log.error("사용자 제재 처리 실패 - userId: {}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
}
